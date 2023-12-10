package org.example

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

data class HeartbeatMessage(val leaderId: String)
data class LeaderElectionRequest(val nodeId: String)
data class VoteRequest(val term: Int, val candidateId: String)
data class VoteResponse(val term: Int, val voteGranted: Boolean, val nodeIdentifier: String)

interface DiscoveryUpcallHandler {
    fun onServiceDiscovered(message: String)
    fun onLeaderElectionHandled()
}

object DiscoveryMW {
    private val logger: Logger = LoggerFactory.getLogger(DiscoveryMW::class.java)
    private var connection: Connection? = null
    private var channel: Channel? = null
    val objectMapper: ObjectMapper = jacksonObjectMapper()
    lateinit var kafkaProducer: KafkaProducer<String, String>
    private lateinit var kafkaConsumer: KafkaConsumer<String, String>
    val discoveryTopic = "discovery_topic"
    val stateReplicationTopic = "state_replication_topic"
    val leaderHealthTopic = "leader_health_topic"
    private var upcallHandler: DiscoveryUpcallHandler? = null
    private var currentTerm = 0
    private var isCandidate = false
    private var votedFor: String? = null
    var nodeIdentifier = "unique_node_identifier" // Replace with actual node identifier
    private val majorityCount = 3 // Set according to your cluster size
    @Volatile private var heartbeatMonitoringThread: Thread? = null
    private var isLeader = false

    fun initRabbitMQ(host: String, port: Int) {
        val factory = ConnectionFactory()
        factory.host = host
        factory.port = port
        connection = factory.newConnection()
        channel = connection?.createChannel()
    }

    fun initKafka(brokers: String) {
        val props = Properties().apply {
            put("bootstrap.servers", brokers)
            put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
            put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
            put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
            put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
            put("group.id", "discovery_group")
        }
        kafkaProducer = KafkaProducer(props)
        kafkaConsumer = KafkaConsumer(props)
        kafkaConsumer.subscribe(listOf(discoveryTopic, leaderHealthTopic, stateReplicationTopic))
    }

    fun broadcastDiscoveryRequest(requestData: Map<String, String>) {
        val message = objectMapper.writeValueAsString(requestData)
        kafkaProducer.send(ProducerRecord(discoveryTopic, "request", message))
    }

    fun listenForDiscoveryResponses() {
        Thread {
            try {
                while (true) {
                    val records = kafkaConsumer.poll(Duration.ofMillis(100))
                    for (record in records) {
                        logger.info("Received message: ${record.value()}")
                        upcallHandler?.onServiceDiscovered(record.value())
                    }
                }
            } catch (e: Exception) {
                logger.error("Error listening for discovery responses", e)
            }
        }.start()
    }

    fun sendMessageToRabbitMQ(message: String) {
        channel?.basicPublish("", "some_queue", null, message.toByteArray(Charsets.UTF_8))
    }

    fun replicateState(state: Map<String, String>) {
        val message = objectMapper.writeValueAsString(state)
        kafkaProducer.send(ProducerRecord(stateReplicationTopic, "state", message))
    }

    fun sendLeaderHeartbeat(leaderId: String) {
        val heartbeatMessage = objectMapper.writeValueAsString(HeartbeatMessage(leaderId))
        kafkaProducer.send(ProducerRecord(leaderHealthTopic, leaderId, heartbeatMessage))
    }

    fun monitorLeaderHealth() {
        Thread {
            try {
                while (true) {
                    val records = kafkaConsumer.poll(Duration.ofMillis(1000))
                    for (record in records) {
                        logger.info("Received heartbeat: ${record.value()}")
                        // Process heartbeat messages here
                    }
                }
            } catch (e: Exception) {
                logger.error("Error monitoring leader health", e)
            }
        }.start()
    }

    fun triggerFailover() {
        logger.info("Initiating failover process")
        stopLeaderHeartbeatMonitoring()

        // Broadcast a leader election message with the current node identifier
        val leaderElectionMessage = objectMapper.writeValueAsString(LeaderElectionRequest(nodeIdentifier))
        kafkaProducer.send(ProducerRecord(discoveryTopic, "election", leaderElectionMessage))

        val electedAsLeader = participateInLeaderElection()
        if (electedAsLeader) {
            startSendingLeaderHeartbeats()
        }
    }


    private fun stopLeaderHeartbeatMonitoring() {
        heartbeatMonitoringThread?.interrupt()
        heartbeatMonitoringThread = null
        kafkaConsumer.unsubscribe()
    }

    private fun participateInLeaderElection(): Boolean {
        currentTerm++
        isCandidate = true
        votedFor = nodeIdentifier

        broadcastVoteRequest(currentTerm)

        val votesReceived = mutableMapOf<String, Boolean>()
        val electionTimeout = Duration.ofMillis(500)
        val startTime = System.currentTimeMillis()
        while (true) {
            val remainingTime = electionTimeout.toMillis() - (System.currentTimeMillis() - startTime)
            if (remainingTime <= 0) {
                break
            }

            val records = kafkaConsumer.poll(Duration.ofMillis(remainingTime))
            for (record in records) {
                val message = objectMapper.readValue<VoteResponse>(record.value())
                votesReceived[message.nodeIdentifier] = message.voteGranted
            }

            if (votesReceived.filterValues { it }.size >= majorityCount) {
                logger.info("Elected as leader!")
                isLeader = true
                return true
            }
        }

        logger.info("Failed to become leader in this term")
        isCandidate = false
        return false
    }

    private fun startSendingLeaderHeartbeats() {
        val heartbeatInterval = Duration.ofMillis(200)
        heartbeatMonitoringThread = Thread {
            try {
                while (isLeader) {
                    sendLeaderHeartbeat(nodeIdentifier)
                    Thread.sleep(heartbeatInterval.toMillis())
                }
            } catch (e: InterruptedException) {
                logger.info("Leader heartbeat thread interrupted")
            }
        }
        heartbeatMonitoringThread?.start()
    }

    fun broadcastVoteRequest(term: Int) {
        val voteRequest = objectMapper.writeValueAsString(VoteRequest(term, nodeIdentifier))
        kafkaProducer.send(ProducerRecord(discoveryTopic, "vote", voteRequest))
    }

    fun setUpcallHandler(handler: DiscoveryUpcallHandler) {
        upcallHandler = handler
    }
}

package org.example

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.example.DiscoveryMW.objectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class DiscoveryMWTest {

    @Test
    fun testBroadcastDiscoveryRequest() {
        val mockProducer = mock(KafkaProducer::class.java)
        DiscoveryMW.kafkaProducer = mockProducer as KafkaProducer<String, String>
        val requestData = mapOf("request" to "discover_services")
        val expectedMessage = objectMapper.writeValueAsString(requestData)
        DiscoveryMW.broadcastDiscoveryRequest(requestData)
        verify(mockProducer).send(ProducerRecord(DiscoveryMW.discoveryTopic, "request", expectedMessage))
    }

    @Test
    fun testReplicateState() {
        val mockProducer = mock(KafkaProducer::class.java)
        DiscoveryMW.kafkaProducer = mockProducer as KafkaProducer<String, String>
        val state = mapOf("service" to "ExampleService", "status" to "Active")
        val expectedMessage = objectMapper.writeValueAsString(state)
        DiscoveryMW.replicateState(state)
        verify(mockProducer).send(ProducerRecord(DiscoveryMW.stateReplicationTopic, "state", expectedMessage))
    }

    @Test
    fun testSendLeaderHeartbeat() {
        val mockProducer = mock(KafkaProducer::class.java)
        DiscoveryMW.kafkaProducer = mockProducer as KafkaProducer<String, String>
        val leaderId = "leader-1"
        val expectedMessage = objectMapper.writeValueAsString(HeartbeatMessage(leaderId))
        DiscoveryMW.sendLeaderHeartbeat(leaderId)
        verify(mockProducer).send(ProducerRecord(DiscoveryMW.leaderHealthTopic, leaderId, expectedMessage))
    }

    @Test
    fun testBroadcastVoteRequest() {
        val mockProducer = mock(KafkaProducer::class.java)
        DiscoveryMW.kafkaProducer = mockProducer as KafkaProducer<String, String>
        val term = 1
        val expectedMessage = objectMapper.writeValueAsString(VoteRequest(term, DiscoveryMW.nodeIdentifier))
        DiscoveryMW.broadcastVoteRequest(term)
        verify(mockProducer).send(ProducerRecord(DiscoveryMW.discoveryTopic, "vote", expectedMessage))
    }
}
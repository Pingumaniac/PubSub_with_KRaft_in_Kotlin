import com.rabbitmq.client._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object EtcdNodeMW {
  private var connection: Connection = _
  private var channel: Channel = _
  private var isLeader: Boolean = false
  private var currentTerm: Int = 0
  private val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  // Initializes the middleware components for Etcd operations
  def init(host: String, port: Int, exchangeName: String): Unit = {
    val factory = new ConnectionFactory()
    factory.setHost(host)
    factory.setPort(port)
    connection = factory.newConnection()
    channel = connection.createChannel()
    channel.exchangeDeclare(exchangeName, "fanout")
  }

  // Handles the replication of log entries across nodes
  def handle_log_replication(entry: RaftConsensusMW.LogEntry, exchangeName: String): Unit = {
    if (isLeader) {
      val message = objectMapper.writeValueAsString(entry)
      channel.basicPublish(exchangeName, "", null, message.getBytes("UTF-8"))
    }
  }

  // Conduct leader election through Etcd
  def conduct_leader_election(exchangeName: String): Unit = {
    currentTerm += 1
    val voteRequest = RaftConsensusMW.VoteRequest(currentTerm)
    val message = objectMapper.writeValueAsString(voteRequest)
    channel.basicPublish(exchangeName, "", null, message.getBytes("UTF-8"))

    val voteCount = mutable.Map[Int, Int]().withDefaultValue(0)
    val queueName = channel.queueDeclare().getQueue
    channel.queueBind(queueName, exchangeName, "")
    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
        val response = objectMapper.readValue(body, classOf[RaftConsensusMW.VoteResponse])
        if (response.term > currentTerm) {
          currentTerm = response.term
          isLeader = false
        } else if (response.term == currentTerm) {
          voteCount(response.term) += (if (response.voteGranted) 1 else 0)
        }
      }
    }
    channel.basicConsume(queueName, true, consumer)

    val timeout = 10.seconds
    Await.result(Future {
      Thread.sleep(timeout.toMillis)
    }, timeout)

    isLeader = voteCount(currentTerm) > (majorityThreshold)
    channel.queueDelete(queueName)
  }

  // Synchronizes the state with other nodes after failover or recovery
  def synchronize_state(): Unit = {
    // Check if a leader exists
    if (isLeader) {
      // Fetch latest state from Etcd
      val stateKey = "raft_state" // The key where the state is stored in Etcd
      val response = etcdClient.getKVClient.get(ByteSequence.from(stateKey.getBytes)).get()
      val latestState = response.getKvs.asScala.headOption.map(kv => kv.getValue.toStringUtf8)
      latestState.foreach(stateJson => {
        // Deserialize the JSON string into the appropriate state object
        val state = objectMapper.readValue(stateJson, classOf[RaftConsensusMW.State])
        // Apply this state to the local state machine
        EtcdConsensusMW.applyState(state)
      })
    } else {
      // Send a request to the leader to retrieve the latest state
      val message = "RequestState"
      channel.basicPublish("", leaderExchangeName, null, message.getBytes("UTF-8"))

      // Listen for the response from the leader
      val consumer = channel.basicConsume("", true, new DefaultConsumer(channel) {
        override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
          val state: RaftConsensusMW.State = objectMapper.readValue(body, classOf[RaftConsensusMW.State])
          // Apply the received state
          EtcdConsensusMW.applyState(state)
        }
      })
      // Wait for the leader's response
      val timeoutDuration = 10.seconds
      val responseReceived = new AtomicBoolean(false)

      val queueName = channel.queueDeclare().getQueue
      channel.queueBind(queueName, leaderExchangeName, "")
      val consumer2 = new DefaultConsumer(channel) {
        override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
          responseReceived.set(true)
          val state = objectMapper.readValue(body, classOf[RaftConsensusMW.State])
          RaftConsensusMW.applyState(state)
          channel.queueDelete(queueName)
        }
      }
      channel.basicConsume(queueName, true, consumer2)

      // Wait for the response with a timeout
      val executor = Executors.newSingleThreadExecutor()
      try {
        executor.submit(() => Thread.sleep(timeoutDuration.toMillis)).get(timeoutDuration.toMillis, TimeUnit.MILLISECONDS)
        if (!responseReceived.get()) {
          // Handle the case where no response is received within the timeout
          println("No response received from the leader within the timeout period.")
          // Additional logic like retrying or error handling
        }
      } catch {
        case e: TimeoutException => println("Timeout while waiting for leader response.")
        case e: Exception => println("Exception occurred: " + e.getMessage)
      } finally {
        executor.shutdownNow()
        if (!responseReceived.get()) {
          channel.queueDelete(queueName)
        }
      }

      consumer.close()
    }
  }
  private def requestVotesFromOtherNodes(): Unit = {
    // Logic to send vote requests to other nodes in the cluster
    // This is a placeholder; actual implementation will involve network communication
    val votes = sendVoteRequestsToOtherNodes(currentTerm) // Replace with actual vote collection logic
    isLeader = votes > majorityThreshold
  }

  // RabbitMQ communication function to send and collect votes
  private def sendVoteRequestsToOtherNodes(term: Int): Int = {
    val voteResponses = mutable.ListBuffer[Boolean]()
    val exchangeName = "raft_exchange" // The exchange used for RAFT communication

    // Send vote request to all nodes
    val voteRequest = RaftConsensusMW.VoteRequest(term)
    val message = objectMapper.writeValueAsString(voteRequest)
    channel.basicPublish(exchangeName, "", null, message.getBytes("UTF-8"))

    // Set up a consumer to listen for vote responses
    val queueName = channel.queueDeclare().getQueue
    channel.queueBind(queueName, exchangeName, "") // Bind to the RAFT exchange
    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
        val response = objectMapper.readValue(body, classOf[RaftConsensusMW.VoteResponse])
        voteResponses += response.voteGranted
      }
    }
    channel.basicConsume(queueName, true, consumer)

    // Wait for a specific timeout to collect votes
    val timeout = 5.seconds
    try {
      Await.result(Future {
        Thread.sleep(timeout.toMillis)
      }, timeout)
    } finally {
      channel.queueDelete(queueName) // Clean up the temporary queue
    }

    // Count the number of positive votes
    voteResponses.count(_ == true)
  }
}

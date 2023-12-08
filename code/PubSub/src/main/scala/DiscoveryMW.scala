import com.rabbitmq.client._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import mousio.etcd4j.EtcdClient
import java.net.URI

object DiscoveryMW {
  private var connection: Connection = _
  private var channel: Channel = _
  private val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)
  private val exchangeName = "discovery_exchange"
  private val queueName = "discovery_queue"
  private val etcdClient = new EtcdClient(URI.create("http://etcd-server:2379")) // Replace with actual Etcd server URI

  // Initialize connection and channel
  def init(host: String, port: Int): Unit = {
    val factory = new ConnectionFactory()
    factory.setHost(host)
    factory.setPort(port)
    connection = factory.newConnection()
    channel = connection.createChannel()
    channel.exchangeDeclare(exchangeName, "fanout")
    channel.queueDeclare(queueName, false, false, false, null)
    channel.queueBind(queueName, exchangeName, "")
  }

  def setup(): Unit = {
    logger.info("Setting up Discovery Middleware")
    // Create a durable, non-exclusive, non-auto-delete queue
    val durable = true  // Queue will survive a broker restart
    val exclusive = false  // Queue can be used by other connections
    val autoDelete = false  // Queue will not be deleted when last consumer unsubscribes
    channel.queueDeclare(queueName, durable, exclusive, autoDelete, null)
    // Bind the queue to another exchange with a specific routing key
    channel.queueBind(queueName, anotherExchangeName, routingKey)
  }


  // Sends out a discovery request to find other services
  def broadcast_discovery_request(requestData: Map[String, String]): Unit = {
    val message = objectMapper.writeValueAsString(requestData)
    channel.basicPublish(exchangeName, "", null, message.getBytes("UTF-8"))
  }

  // Listens for responses to discovery requests
  def listen_for_discovery_responses(callback: String => Unit): Unit = {
    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
        val message = new String(body, "UTF-8")
        callback(message)
      }
    }
    channel.basicConsume(queueName, true, consumer)
  }

  // Replicates the service state to other nodes via Etcd (Placeholder)
  def replicate_state(state: Map[String, String]): Unit = {
    logger.info("Replicating state to other nodes")
    state.foreach { case (key, value) =>
      val etcdKey = ByteSequence.from(key.getBytes(StandardCharsets.UTF_8))
      val etcdValue = ByteSequence.from(value.getBytes(StandardCharsets.UTF_8))
      etcdClient.put(etcdKey, etcdValue).get()
    }
  }


  // Continuously monitors the health of the leader node (Placeholder)
  def monitor_leader_health(): Unit = {
    logger.info("Monitoring health of the leader node")
    // Example: Periodically check the leader's health status in Etcd
    val leaderHealthKey = "/raft/leader/health"
    val watcher = etcdClient.getWatchClient.watch(ByteSequence.from(leaderHealthKey.getBytes(StandardCharsets.UTF_8)), new Watch.Listener() {
      override def onNext(response: WatchResponse): Unit = {
        // Handle the leader's health status update
        val healthStatus = response.getKeyValue.getValue.toString(StandardCharsets.UTF_8)
        // Implement specific actions based on the health status
      }
      override def onError(e: Throwable): Unit = logger.error("Error watching leader health", e)
      override def onCompleted(): Unit = logger.info("Completed watching leader health")
    })
  }


  // Initiates a failover process in case of leader failure (Placeholder)
  def trigger_failover(): Unit = {
    logger.info("Initiating failover process")
    // Trigger leader election in EtcdNodeMW
    EtcdNodeMW.conduct_leader_election()
  }
}

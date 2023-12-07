import com.rabbitmq.client.{AMQP, Channel, Connection, ConnectionFactory, DefaultConsumer, Envelope, MessageProperties}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
trait SubUpcallHandler {
  def onMessageReceived(message: String): Unit
  def onErrorOccurred(error: Throwable): Unit
}

object SubMW {
  private var connection: Connection = _
  private var channel: Channel = _
  private var queueName: String = _
  private var upcallHandler: Option[SubUpcallHandler] = None

  private val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  def connect_to_server(host: String, port: Int): Unit = {
    val factory = new ConnectionFactory()
    factory.setHost(host)
    factory.setPort(port)
    connection = factory.newConnection()
    channel = connection.createChannel()
  }

  def setup_subscriber_queue(exchangeName: String): Unit = {
    queueName = channel.queueDeclare().getQueue
    channel.queueBind(queueName, exchangeName, "")

    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
        try {
          val message = new String(body, "UTF-8")
          val deserializedMessage : String = objectMapper.readValue(message, classOf[String]) 
          upcallHandler.foreach(_.onMessageReceived(deserializedMessage))
        } catch {
          case e: Exception =>
            upcallHandler.foreach(_.onErrorOccurred(e))
        }
      }
    }
  }
    
  def set_upcall_handle(handler: SubUpcallHandler): Unit = {
    upcallHandler = Some(handler)
  }

  def unsubscribe(exchangeName: String): Unit = {
    if (channel != null && queueName != null) {
      // Unbind the queue from the specified exchange
      channel.queueUnbind(queueName, exchangeName, "")
    }
  }

  def change_subscription(newExchangeName: String): Unit = {
    unsubscribe("")
    setup_subscriber_queue(newExchangeName)
  }

  def disconnect(): Unit = {
    if (channel != null && queueName != null) {
      channel.queueDelete(queueName)
      channel.close()
    }
    if (connection != null) connection.close()
  }
}
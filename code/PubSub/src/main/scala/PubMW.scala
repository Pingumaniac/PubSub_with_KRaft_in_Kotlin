import com.rabbitmq.client.{Connection, ConnectionFactory, Channel, MessageProperties}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

// Define a trait for handling upcall events
trait UpcallHandler {
  def onMessagePublished(topic: String, message: String): Unit
  def onErrorOccurred(error: Throwable): Unit
}

object PubMW {
  private var connection: Connection = _
  private var channel: Channel = _
  private var currentTopic: String = _
  private var upcallHandler: Option[UpcallHandler] = None

  // Initialize ObjectMapper for JSON serialization
  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def connect_to_server(host: String, port: Int): Unit = {
    val factory = new ConnectionFactory()
    factory.setHost(host)
    factory.setPort(port)
    connection = factory.newConnection()
    channel = connection.createChannel()
  }

  def setup_message_exchange(exchangeName: String): Unit = {
    channel.exchangeDeclare(exchangeName, "fanout", true)
    currentTopic = exchangeName
  }

  def publish_message(message: String): Unit = {
    try {
      val serializedMessage = serializeToJsonBytes(message)
      val properties = MessageProperties.PERSISTENT_TEXT_PLAIN

      if (currentTopic.isEmpty) {
        throw new IllegalStateException("Exchange not initialized")
      }

      channel.basicPublish(currentTopic, "", properties, serializedMessage)
      upcallHandler.foreach(_.onMessagePublished(currentTopic, message))
    } catch {
      case e: Throwable =>
        upcallHandler.foreach(_.onErrorOccurred(e))
    }
  }

  def set_upcall_handle(handler: UpcallHandler): Unit = {
    upcallHandler = Some(handler)
  }

  def close_connection(): Unit = {
    if (channel != null) channel.close()
    if (connection != null) connection.close()
  }

  private def serializeToJsonBytes(obj: Any): Array[Byte] = {
    mapper.writeValueAsBytes(obj)
  }
}

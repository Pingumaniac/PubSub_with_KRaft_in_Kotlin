import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.rabbitmq.client.{AMQP, Connection, ConnectionFactory, Channel, MessageProperties}

object JsonSerializationUtil {
  private val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  def serializeToJsonBytes(obj: Any): Array[Byte] = {
    mapper.writeValueAsBytes(obj)
  }
}

object PubMW {
  private var connection: Connection = _
  private var channel: Channel = _
  private var currentTopic: String = _

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
    val serializedMessage = JsonSerializationUtil.serializeToJsonBytes(message)
    val properties = MessageProperties.PERSISTENT_TEXT_PLAIN

    if (currentTopic.isEmpty) {
      throw new IllegalStateException("Exchange not initialized")
    }

    channel.basicPublish(currentTopic, "", properties, serializedMessage)
  }

  def close_connection(): Unit = {
    if (channel != null) channel.close()
    if (connection != null) connection.close()
  }
}

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Connection
import com.rabbitmq.client.Channel


object PubMW {
  private var connection: Connection = _
  private var channel: Channel = _

  def connect_to_server(host: String, port: Int): Unit = {
    connection = ConnectionFactory().newConnection(host, port)
    channel = connection.createChannel()
  }

  def setup_message_exchange(exchangeName: String): Unit = {
    channel.exchangeDeclare(exchangeName, "fanout", durable = true)
  }

  def publish_message(message: Any, exchangeName: String): Unit = {
    val serializedMessage = message.toString().getBytes
    channel.basicPublish(exchangeName, "", null, serializedMessage)
  }

  def need_reconfig_for_topic(topic: String): Boolean = {
    // Implement logic to determine if reconfiguration is needed based on current topic
    // and provided topic
  }

  def reconfigure_for_topic(topic: String): Unit = {
    // Implement logic to reconfigure the exchange and associated resources
    // based on the new topic
  }

  def close_connection(): Unit = {
    if (channel != null) channel.close()
    if (connection != null) connection.close()
  }
}
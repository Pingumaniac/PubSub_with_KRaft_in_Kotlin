import com.rabbitmq.client.{AMQP, Channel, Connection, DefaultConsumer}

object SubMW {
  private var connection: Connection = _
  private var channel: Channel = _
  private var queueName: String = _

  def connect_to_server(host: String, port: Int): Unit = {
    connection = ConnectionFactory().newConnection(host, port)
    channel = connection.createChannel()
  }

  def setup_subscriber_queue(exchangeName: String, callback: (Any) => Unit): Unit = {
    queueName = channel.queueDeclare().getQueue
    channel.queueBind(queueName, exchangeName, "")

    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: AMQP.Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
        val message = new String(body)
        callback(message)
      }
    }

    channel.basicConsume(queueName, true, consumer)
  }

  def listen_for_messages(callback: (Any) => Unit): Unit = {
    // Implement logic to listen for messages on the subscribed queue
    // and call the provided callback with the received message
  }

  // Additional methods for managing subscriptions and error handling
}

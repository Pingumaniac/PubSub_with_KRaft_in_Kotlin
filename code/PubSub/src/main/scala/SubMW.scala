import com.rabbitmq.client.{AMQP, Channel, Connection, ConnectionFactory, DefaultConsumer, Envelope, MessageProperties}

object SubMW {
  private var connection: Connection = _
  private var channel: Channel = _
  private var queueName: String = _

  def connect_to_server(host: String, port: Int): Unit = {
    val factory = new ConnectionFactory()
    factory.setHost(host)
    factory.setPort(port)
    connection = factory.newConnection() // Corrected method call
    channel = connection.createChannel()
  }

  def setup_subscriber_queue(exchangeName: String, callback: String => Unit): Unit = {
    queueName = channel.queueDeclare().getQueue
    channel.queueBind(queueName, exchangeName, "")

    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
        try {
          val message = new String(body, "UTF-8")
          callback(message)
        } catch {
          case e: Exception =>
            println(s"Error while processing message: ${e.getMessage}")
        }
      }
    }

    channel.basicConsume(queueName, true, consumer)
  }
}
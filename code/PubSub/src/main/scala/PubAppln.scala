import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.util.control.NonFatal

class PubAppln extends UpcallHandler {
  private val logger: Logger = LoggerFactory.getLogger(getClass)
  private val mw_obj = PubMW
  mw_obj.set_upcall_handle(this)

  private var name: String = _
  private var num_topics: Int = _
  private var topiclist: List[String] = _
  private var iters: Int = _
  private var frequency: Int = _

  def init(name: String, num_topics: Int, topiclist: List[String], iters: Int, frequency: Int): Unit = {
    this.name = name
    this.num_topics = num_topics
    this.topiclist = topiclist
    this.iters = iters
    this.frequency = frequency
  }

  def configure_publisher(settings: Map[String, String]): Unit = {
    val host = settings.getOrElse("host", "localhost")
    val port = settings.getOrElse("port", "5672").toInt
    mw_obj.connect_to_server(host, port)
  }

  private def set_up_topic(topic: String): Unit = {
    mw_obj.setup_message_exchange(topic)
  }

  def publish(message: Any): Unit = {
    // TODO: Implement message serialization logic here.
    val serializedMessage: String = serializeMessage(message)
    mw_obj.publish_message(serializedMessage)
  }

  def change_topic(newTopic: String): Unit = {
    set_up_topic(newTopic)
  }

  def shutdown_publisher(): Unit = {
    mw_obj.close_connection()
  }

  override def onMessagePublished(topic: String, message: String): Unit = {
    logger.info(s"Message published to topic $topic: $message")
  }

  override def onErrorOccurred(error: Throwable): Unit = {
    logger.error("Error occurred in PubMW", error)
  }

  def dump(): Unit = {
    try {
      logger.info("PubAppln::dump")
      logger.info(s"-Name: $name")
      logger.info(s"-Number of Topics: $num_topics")
      logger.info(s"-TopicList: ${topiclist.mkString(", ")}")
      logger.info(s"-Iterations: $iters")
      logger.info(s"-Frequency: $frequency")
    } catch {
      case NonFatal(e) => logger.error("Error during dump", e)
    }
  }

  // Placeholder for the serialization logic
  private def serializeMessage(message: Any): String = {
    // Implement your serialization logic here
    // For now, let's just return a simple string representation
    message.toString
  }
}

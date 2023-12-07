import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.util.control.NonFatal

class SubAppln extends SubUpcallHandler {
  private val logger: Logger = LoggerFactory.getLogger(getClass)
  private val mw_obj = SubMW
  mw_obj.set_upcall_handle(this)

  // State variables
  private var name: String = _
  private var subscribedExchanges: Set[String] = Set()

  def init(name: String, exchanges: Set[String]): Unit = {
    this.name = name
    this.subscribedExchanges = exchanges
  }

  def subscribe(exchangeName: String): Unit = {
    mw_obj.setup_subscriber_queue(exchangeName)
    subscribedExchanges += exchangeName
  }

  def unsubscribe(exchangeName: String): Unit = {
    mw_obj.unsubscribe(exchangeName)
    subscribedExchanges -= exchangeName
  }

  def change_subscription(newExchangeName: String): Unit = {
    subscribedExchanges.foreach(unsubscribe)
    subscribe(newExchangeName)
  }

  def disconnect(): Unit = {
    mw_obj.disconnect()
  }

  override def onMessageReceived(message: String): Unit = {
    logger.info(s"Message received: $message")
  }

  override def onErrorOccurred(error: Throwable): Unit = {
    logger.error("Error occurred in SubMW", error)
  }

  def dump(): Unit = {
    try {
      logger.info("SubAppln::dump")
      logger.info(s"-Name: $name")
      logger.info(s"-Subscribed Exchanges: ${subscribedExchanges.mkString(", ")}")
    } catch {
      case NonFatal(e) => logger.error("Error during dump", e)
    }
  }
}

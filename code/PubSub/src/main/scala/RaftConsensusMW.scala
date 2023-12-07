import com.rabbitmq.client._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object RaftConsensusMW {
  private var connection: Connection = _
  private var channel: Channel = _
  private var log: List[LogEntry] = List()
  private val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  case class LogEntry(data: String, term: Int, isCommitted: Boolean = false)

  // Initialize connection and channel for RAFT consensus operations
  def init(host: String, port: Int, exchangeName: String): Unit = {
    val factory = new ConnectionFactory()
    factory.setHost(host)
    factory.setPort(port)
    connection = factory.newConnection()
    channel = connection.createChannel()
    channel.exchangeDeclare(exchangeName, "fanout")
  }

  // Starts the RAFT consensus process for decision making
  def start_consensus_process(): Unit = {
    // Logic to start the Raft consensus process
    // Including leader election and log replication
  }

  // Appends a new entry to the RAFT log and broadcasts it to other nodes
  def append_entry_to_log(entry: LogEntry, exchangeName: String): Unit = {
    log = log :+ entry
    val message = objectMapper.writeValueAsString(entry)
    channel.basicPublish(exchangeName, "", null, message.getBytes("UTF-8"))
  }

  // Commits an entry to the RAFT log after reaching consensus
  def commit_entry(index: Int): Unit = {
    if (index >= 0 && index < log.length) {
      val updatedEntry = log(index).copy(isCommitted = true)
      log = log.updated(index, updatedEntry)
      // Additional logic to handle the committed entry (e.g., apply to state machine)
    } else {
      // Handle invalid index
      println(s"Invalid log index: $index")
    }
  }
}

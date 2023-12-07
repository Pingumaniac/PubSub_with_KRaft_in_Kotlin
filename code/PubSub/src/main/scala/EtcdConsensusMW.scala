import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import io.etcd.jetcd.{ByteSequence, Client, KV}
import io.etcd.jetcd.kv.{PutResponse, Response}
import io.etcd.jetcd.options.{GetOption, PutOption, WatchOption}
import io.etcd.jetcd.Watch.Watcher
import io.etcd.jetcd.watch.WatchResponse
import java.nio.charset.StandardCharsets

object EtcdConsensusMW {
  private val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)
  private var client: Client = _
  private var kvClient: KV = _
  private var log: List[LogEntry] = List()
  private var currentLeader: String = _
  private var stateMachine: StateMachine = _

  // Required for executing future operations
  implicit val ec: ExecutionContext = ExecutionContext.global

  // Initialize etcd client and key-value store
  def init(etcdUrl: String, stateMachine: StateMachine): Unit = {
    client = Client.builder().endpoints(etcdUrl).build()
    kvClient = client.getKVClient
    this.stateMachine = stateMachine
  }

  // Leader election
  def start_consensus_process(): Unit = {
    val leaderKey = "/raft/leader"
    val leaderValue = ByteSequence.from("node-id", StandardCharsets.UTF_8) // Replace "node-id" with actual node identifier

    Future {
      kvClient.put(ByteSequence.from(leaderKey, StandardCharsets.UTF_8), leaderValue, PutOption.newBuilder().withLeaseId(leaseId).build()).get()
    }.onComplete {
      case Success(_) => currentLeader = "node-id" // Again, replace with actual node identifier
      case Failure(exception) => println("Failed to elect leader: " + exception.getMessage)
    }
  }

  // Start leader duties
  private def start_leader_duties(revision: Long): Unit = {
    // Periodically send heartbeat messages to followers
    // Logic to handle follower responses and timeouts
    // Watch for new entries to the log
    val logWatcher: Watcher = client.getWatchClient.watch("/raft/log/", new Watch.Listener() {
      override def onNext(response: WatchResponse): Unit = {
        val newEntry = objectMapper.readValue(response.getKeyValue.getValue().getBytes(StandardCharsets.UTF_8), classOf[LogEntry])
        append_entry_to_log(newEntry)
      }
      override def onError(e: Throwable): Unit = {
        println(s"Error while watching for new entries: ${e.getMessage}")
      }
      override def onCompleted(): Unit = {
        println("Watcher for new entries completed")
      }
    })
  }

  // Handles incoming messages based on type
  def handle_message(message: String): Unit = {
    val entry = objectMapper.readValue(message, classOf[LogEntry])
    entry.term match {
      case currentLeaderTerm if currentLeaderTerm == entry.term =>
        entry.data match {
          case VoteRequest => process_vote_request(entry)
          case AppendEntryRequest => process_append_entry_request(entry)
          case CommitEntryNotification =>
            if (entry.isCommitted) {
              stateMachine.apply(entry)
              publish_commit_notification(entry)
            }
          case _ =>
            println(s"Unknown message type received: ${entry.data}")
        }
      case _ =>
        process_outdated_message(entry)
    }
  }

  def process_vote_request(entry: LogEntry): Unit = {
    if (currentLeaderTerm > entry.term) {
      // Reject vote request with higher term
      send_vote_response(entry.leaderId, entry.term, false)
    } else if (currentLeaderTerm == entry.term) {
      // Already voted for this term, reject the request
      send_vote_response(entry.leaderId, entry.term, false)
    } else {
      // Check if candidate's log is at least as up-to-date as receiver's log
      if (is_candidate_log_up_to_date(entry)) {
        // Grant vote to the candidate
        send_vote_response(entry.leaderId, entry.term, true)
        currentLeader = entry.leaderId
        currentLeaderTerm = entry.term
      } else {
        // Deny vote request because candidate's log is not up-to-date
        send_vote_response(entry.leaderId, entry.term, false)
      }
    }

    def process_append_entry_request(entry: LogEntry): Unit = {
      if (currentLeaderTerm > entry.term) {
        // Reject append entry request with higher term
        send_append_entry_response(entry.leaderId, entry.term, false, -1)
      } else if (currentLeaderTerm == entry.term) {
        // Check if leader is the current leader
        if (entry.leaderId == currentLeader) {
          // Check if entry is consistent with the current log
          if (is_entry_consistent(entry)) {
            append_entry_to_log(entry)
            update_follower_state(entry)
            send_append_entry_response(entry.leaderId, entry.term, true, entry.index)
          } else {
            // Entry is not consistent, find conflict index
            val conflictIndex = find_conflict_index(entry)
            send_append_entry_response(entry.leaderId, entry.term, false, conflictIndex)
          }
        } else {
          // Leader has changed, reject the request
          send_append_entry_response(entry.leaderId, entry.term, false, -1)
        }
      } else {
        // Update current leader and term
        currentLeader = entry.leaderId
        currentLeaderTerm = entry.term
        process_append_entry_request(entry) // Process request again with updated information
      }
    }
  }

  def process_append_entry_request(entry: LogEntry): Unit = {
    if (currentLeaderTerm > entry.term) {
      // Reject append entry request with higher term
      send_append_entry_response(entry.leaderId, entry.term, false, -1)
    } else if (currentLeaderTerm == entry.term) {
      // Check if leader is the current leader
      if (entry.leaderId == currentLeader) {
        // Check if entry is consistent with the current log
        if (is_entry_consistent(entry)) {
          append_entry_to_log(entry)
          update_follower_state(entry)
          send_append_entry_response(entry.leaderId, entry.term, true, entry.index)
        } else {
          // Entry is not consistent, find conflict index
          val conflictIndex = find_conflict_index(entry)
          send_append_entry_response(entry.leaderId, entry.term, false, conflictIndex)
        }
      } else {
        // Leader has changed, reject the request
        send_append_entry_response(entry.leaderId, entry.term, false, -1)
      }
    } else {
      // Update current leader and term
      currentLeader = entry.leaderId
      currentLeaderTerm = entry.term
      process_append_entry_request(entry) // Process request again with updated information
    }
  }

  // Start follower duties
  private def start_follower_duties(term: Int): Unit = {
    // Periodically send heartbeat requests to the leader
    // Implement logic to handle leader responses and timeouts
    // Watch for changes to the leader key
    val leaderWatcher: Watcher = client.getWatchClient.watch("/raft/leader", new Watch.Listener() {
      override def onNext(response: WatchResponse): Unit = {
        if (response.getEvents().getEventType() == Watch.EventType.PUT) {
          val leaderValue = response.getKeyValue().getValue().toString(StandardCharsets.UTF_8)
          if (leaderValue != currentLeader) {
            currentLeader = leaderValue
            // Update leader information and continue follower duties
            start_follower_duties(term)
          }
        }
      }
      override def onError(e: Throwable): Unit = {
        println(s"Error while watching for leader changes: ${e.getMessage}")
      }
      override def onCompleted(): Unit = {
        println("Watcher for leader changes completed")
      }
    })
  }

  private def publish_commit_notification(entry: LogEntry): Unit = {
    // Notify other nodes about the committed entry
    // This can be done through Etcd by setting a specific key-value
    val commitKey = ByteSequence.from(s"/raft/commit/${entry.index}", StandardCharsets.UTF_8)
    val commitValue = ByteSequence.from(objectMapper.writeValueAsString(entry), StandardCharsets.UTF_8)
    kvClient.put(commitKey, commitValue).get()
  }

  private def process_outdated_message(entry: LogEntry): Unit = {
    // Log the message for audit purposes
    log.info(s"Received outdated message: ${entry.toString}")
  }

  // Append a new entry to the log and store it in etcd
  def append_entry_to_log(entry: LogEntry): Unit = {
    log = log :+ entry
    val serializedEntry = objectMapper.writeValueAsString(entry)
    kvClient.put(ByteSequence.from("/raft/log/" + log.length, StandardCharsets.UTF_8),
      ByteSequence.from(serializedEntry, StandardCharsets.UTF_8)).get()
  }

  // Commits an entry to the RAFT log and notifies other nodes
  def commit_entry(index: Int): Unit = {
    if (index >= 0 && index < log.length) {
      val updatedEntry = log(index).copy(isCommitted = true)
      log = log.updated(index, updatedEntry)
      stateMachine.apply(updatedEntry)
      kvClient.put(ByteSequence.from("/raft/commit/" + index, StandardCharsets.UTF_8),
        ByteSequence.from("true", StandardCharsets.UTF_8)).get()
    } else {
      println(s"Invalid log index: $index")
    }
  }
}

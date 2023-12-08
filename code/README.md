# About Software Architecture

## Overall architecture
I have implemented a Publisher/Subscriber architecture in Scala  using the Remote Procedure Call (RPC) pattern. To achieve better decoupling, I split the code into two main categories:
* ~Appln.scala: Contains all application-level logic.
* ~MW.scala: Contains all middleware-level code, including RabbitMQ communication (~MW files).

While RabbitMQ excels at messaging, it lacks anonymity. Subscribers must explicitly connect to publishers by IP and port, compromising the ideal decoupling of publish/subscribe (time, space, and synchronization independence).

Therefore, I sought a solution where publishers and subscribers remain anonymous to each other. Of course, someone needs to manage these associations, technically breaking the "ideal" definition, but as long as the application logic adheres to it, that's acceptable.

To achieve this, I designed a middleware layer. Instead of directly using RabbitMQ, application logic (~Appln.scala files) interacts with the middleware's API (~MW.scala files). This lightweight pub/sub middleware sits on top of RabbitMQ, enabling anonymity.

Furthermore, I've implemented warm-passive fault tolerance for the Discovery service using the Etcd consensus protocol for coordination.

Finally, shared code is organized in:
* CommonAppln.scala: Holds methods used by other ~Appln.scala files.

This approach promotes cleaner separation of concerns, improves anonymity and fault tolerance, and ultimately makes the system more robust and scalable.

## Description of major functions in ~Appln.scala

1. PubAppln.scala
* configure_publisher : Configures the publisher with necessary settings
* set_up_topic: set up the topic for the publisher to publish
* publish : Publishes a message without needing to know the subscribers.like topics.
* change_topic: change the topic for the publisher to publish
* set_log_level: customise log level
* shutdown_publisher: close Publisher from Application level

2. SubAppln.scala
* subscribe : Subscribes to a given topic and defines a callback for handling received messages.
* unsubscribe: Unsubscribe from a specific topi
* disconnect : Unsubscribes from the current subscription.
* change_subscription: Change the set of subscribed topics
* handle_listen_errors: Error handling for message listening

3. DiscoveryAppln.scala
* discover_services : Discovers available services in the network.
* register_service : Registers a new service with the discovery system.
* update_service_state : Updates the state of the service, used in sync with Etcd.
* handle_leader_election : Handles the process of leader election in case of failover.
* apply_log_entry : Applies a log entry from the Etcd log to update the state.

4. EtcdNodeAppln.scala
* initialize_node : Initializes the Etcd node for participating in the consensus.
* propagate_update : Propagates updates to the Etcd log.
* perform_health_check : Performs health checks on the leader node and initiates failover if needed.
* handle_state_transition : Manages transitions between leader and follower states.
* recover_from_failure : Handles the process of rejoining and syncing with the cluster after recovery.

5. CommonAppln.scala
* log_entry : Represents a log entry data structure used in the Etcd algorithm.
* state_machine : Represents the state machine logic used across the application.
* apply_log_entry : Applies a given log entry to the state machine.
* get_current_state : Retrieves the current state of the state machine.
* update_state : Updates the state machine with a new state.

## Description of major functions in ~MW.scala

1. PubMW.scala
* connect_to_server : Function to establish a connection to the RabbitMQ server
* setup_message_exchange : Function to declare a publisher exchange
* publish_messge: Function to publish a message to a specific exchange
* need_reconfig_for_topic: Function to publish a message to a specific exchange
* reconfigure_for_topic: Function to reconfigure the publisher exchange for a new topic
* close_connection: Function to close the connection to the RabbitMQ server

2. SubMW.scala
* connect_to_server : Function to establish a connection to the RabbitMQ server
* setup_subscriber_queue : Function to declare a subscriber queue and bind it to an exchange
* listen_for_messages : Listens for incoming messages and uses the callback for handling them.

3. DiscoveryMW.scalla
* init: Initialize connection and channe
* setup: Function to declare exchange and queue for discovery
* broadcast_discovery_request : Sends out a discovery request to find other services.
* listen_for_discovery_responses : Listens for responses to discovery requests.
* replicate_state : Replicates the service state to other nodes via Etcd.
* monitor_leader_health : Continuously monitors the health of the leader node.
* trigger_failover : Initiates a failover process in case of leader failure.

4. EtcdNodeMW.scala
* init : Initializes the middleware components for Etcd operations.
* handle_log_replication  Handles the replication of log entries across nodes.
* conduct_leader_election : Conducts a leader election process as per Etcd.
* synchronize_state : Synchronizes the state with other nodes after failover or recovery.

5. EtcdConsensusMW.scala
* init: Innitialize connection and channel for Etcd consensus operations
* start_consensus_process : Starts the Etcd consensus process for decision making.
* append_entry_to_log : Appends a new entry to the Etcd log.
* commit_entry : Commits an entry to the Etcd log after reaching consensus.

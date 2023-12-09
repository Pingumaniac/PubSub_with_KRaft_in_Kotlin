# About Software Architecture

## Overall architecture
I have implemented a Publisher/Subscriber architecture in Scala  using the Remote Procedure Call (RPC) pattern. To achieve better decoupling, I split the code into two main categories:
* ~Appln.kt: Contains all application-level logic.
* ~MW.kt: Contains all middleware-level code, including RabbitMQ communication (~MW files).

While RabbitMQ excels at messaging, it lacks anonymity. Subscribers must explicitly connect to publishers by IP and port, compromising the ideal decoupling of publish/subscribe (time, space, and synchronization independence).

Therefore, I sought a solution where publishers and subscribers remain anonymous to each other. Of course, someone needs to manage these associations, technically breaking the "ideal" definition, but as long as the application logic adheres to it, that's acceptable.

To achieve this, I designed a middleware layer. Instead of directly using RabbitMQ, application logic (~Appln.kt files) interacts with the middleware's API (~MW.kt files). This lightweight pub/sub middleware sits on top of RabbitMQ, enabling anonymity.

Additionally, warm-passive fault tolerance is implemented for the Discovery service using Apache Kafka (uses KRaft distributed consensus algorithm), leveraging its capabilities for distributed systems coordination.

This approach promotes cleaner separation of concerns, improves anonymity and fault tolerance, and ultimately makes the system more robust and scalable.


## Description of major functions in ~Appln.kt

1. PubAppln.kt
* configurePublisher: Configures the publisher with the given settings.
* setUpTopic: Sets up a topic for the publisher.
* publish: Publishes a message to the current topic.
* changeTopic: Changes the current topic for publishing.
* shutdownPublisher: Shuts down the publisher.
* onMessagePublished: Callback for when a message is published.
* onErrorOccurred: Callback for handling errors in the publishing process.
* dump: Logs the current state of the publisher for debugging.

2. SubAppln.kt
* subscribe: Subscribes to a given topic.
* unsubscribe: Unsubscribes from a specific topic.
* changeSubscription: Changes the subscription to a new topic.
* disconnect: Disconnects from the RabbitMQ server.
* onMessageReceived: Callback for handling received messages.
* onErrorOccurred: Callback for handling errors in the subscription process.
* dump: Logs the current state of the subscriber for debugging.

3. DiscoveryAppln.kt
* onServiceDiscovered: Processes the discovery of a new service and adds it to the discovered services set.
* onLeaderElectionHandled: Handles actions after the leader election process is completed.
* discoverServices: Broadcasts a discovery request to discover services in the network.
* registerService: Registers a new service with the discovery system.
* updateServiceState: Updates the state of a service in the discovery system.
* parseServiceInfo: Parses the message string into a ServiceInfo object.
* dump: Logs the current state of discovered services for debugging.


## Description of major functions in ~MW.kt

1. PubMW.kt
* connectToServer: Establishes a connection to the RabbitMQ server.
* setupMessageExchange: Sets up a message exchange on RabbitMQ.
* publishMessage: Publishes a message to RabbitMQ.
* setReconfigForTopic: Determines if reconfiguration is needed for a new topic.
* reconfigureForTopic: Reconfigures RabbitMQ for a new topic.
* closeConnection: Closes the RabbitMQ connection.
* serializeToJsonBytes: Serializes an object to JSON bytes.

2. SubMW.kt
* connectToServer: Establishes a connection to the RabbitMQ server.
* setupSubscriberQueue: Sets up a queue for subscribing to messages.
* listenForMessages: Listens for incoming messages on RabbitMQ.
* unsubscribe: Unsubscribes from a specific topic or queue.
* changeSubscription: Changes the subscription to a new topic or queue.
* disconnect: Disconnects from the RabbitMQ server.

3. DiscoveryMW.kt
* initRabbitMQ: Initializes the RabbitMQ connection and channel.
* initKafka: Sets up Kafka Producer and Consumer with given broker configurations.
* broadcastDiscoveryRequest: Sends a discovery request using Kafka.
listenForDiscoveryResponses: Listens for responses to discovery requests using Kafka.
* sendMessageToRabbitMQ: Sends messages through RabbitMQ as an example.
* replicateState: Replicates state information using Kafka.
* sendLeaderHeartbeat: Sends a leader heartbeat message using Kafka.
* monitorLeaderHealth: Monitors the health of the leader node using Kafka.
* triggerFailover: Initiates a failover process using Kafka.
* setUpcallHandler: Sets the handler for discovery-related upcall events.

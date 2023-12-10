package org.example

fun main() {
    // Initialize Publisher Application
    val pubApp = PubAppln()
    pubApp.initialize("pubapp", 3, listOf("python", "ruby", "typescript"), 1000, 1)

    // Initialize Subscriber Application
    val subApp = SubAppln()

    // Initialize Discovery Application
    val discoveryApp = DiscoveryAppln()

    // Initialize Kafka with default local broker address
    DiscoveryMW.initKafka("localhost:9092")

    // Start each application in separate threads
    val pubThread = Thread { pubApp.publish("ruby", "rails") }
    val subThread = Thread { subApp.initialize("subapp",  setOf("python", "ruby", "typescript")) }
    val discoveryThread = Thread { discoveryApp.discoverServices() }

    pubThread.start()
    subThread.start()
    discoveryThread.start()

    println("All applications have been started.")
}

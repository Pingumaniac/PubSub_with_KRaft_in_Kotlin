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
    val pubThread = Thread {
        pubApp.publish("ruby", "rails")
        pubApp.publish("python", "django")
        pubApp.publish("typescript", "angular")
    }
    val subThread = Thread { subApp.initialize("subapp",  setOf("python", "ruby", "typescript")) }
    val discoveryThread = Thread { discoveryApp.discoverServices() }

    pubThread.start()
    subThread.start()
    discoveryThread.start()

    println("All applications have been started.")
}

fun main2() {
    // Initialize Publisher Application
    val pubApp2 = PubAppln()
    pubApp2.initialize("pubapp", 3, listOf("C", "C++", "C#"), 1000, 1)

    // Initialize Subscriber Application
    val subApp2 = SubAppln()

    // Initialize Discovery Application
    val discoveryApp2 = DiscoveryAppln()

    // Initialize Kafka with default local broker address
    DiscoveryMW.initKafka("localhost:9092")

    // Start each application in separate threads
    val pubThread2 = Thread {
        pubApp2.publish("C", "malloc")
        pubApp2.publish("C++", "calloc")
        pubApp2.publish("C#", "Unity")
    }
    val subThread2 = Thread { subApp2.initialize("subapp",  setOf("C", "F#", "Haskell")) }
    val discoveryThread2 = Thread { discoveryApp2.discoverServices() }

    pubThread2.start()
    subThread2.start()
    discoveryThread2.start()

    println("All applications have been started.")
}

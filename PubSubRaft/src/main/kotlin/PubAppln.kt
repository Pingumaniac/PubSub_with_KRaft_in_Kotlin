package org.example

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PubAppln : UpcallHandler {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val mwObj = PubMW
    private var name: String? = null
    private var numTopics: Int = 0
    private var topicList: List<String> = listOf()
    private var iters: Int = 0
    private var frequency: Int = 0

    init {
        mwObj.setUpcallHandler(this)
    }

    fun init(name: String, numTopics: Int, topicList: List<String>, iters: Int, frequency: Int) {
        this.name = name
        this.numTopics = numTopics
        this.topicList = topicList
        this.iters = iters
        this.frequency = frequency
    }

    fun configurePublisher(settings: Map<String, String>) {
        val host = settings.getOrElse("host") { "localhost" }
        val port = settings.getOrElse("port") { "5672" }.toInt()
        mwObj.connectToServer(host, port)
    }

    private fun setUpTopic(topic: String) {
        mwObj.setupMessageExchange(topic)
    }

    fun publish(message: Any) {
        // TODO: Implement message serialization logic here.
        val serializedMessage: String = serializeMessage(message)
        mwObj.publishMessage(serializedMessage)
    }

    fun changeTopic(newTopic: String) {
        setUpTopic(newTopic)
    }

    fun shutdownPublisher() {
        mwObj.closeConnection()
    }

    override fun onMessagePublished(topic: String, message: String) {
        logger.info("Message published to topic $topic: $message")
    }

    override fun onErrorOccurred(error: Throwable) {
        logger.error("Error occurred in PubMW", error)
    }

    fun dump() {
        try {
            logger.info("PubAppln::dump")
            logger.info("-Name: $name")
            logger.info("-Number of Topics: $numTopics")
            logger.info("-TopicList: ${topicList.joinToString(", ")}")
            logger.info("-Iterations: $iters")
            logger.info("-Frequency: $frequency")
        } catch (e: Exception) {
            logger.error("Error during dump", e)
        }
    }

    // Placeholder for the serialization logic
    private fun serializeMessage(message: Any): String {
        // For now, just return a simple string representation
        return message.toString()
    }
}

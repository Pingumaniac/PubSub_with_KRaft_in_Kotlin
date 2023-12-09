package org.example

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SubAppln : SubUpcallHandler {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val mwObj = SubMW

    init {
        mwObj.setUpcallHandler(this)
    }

    // State variables
    private var name: String? = null
    private var subscribedExchanges: MutableSet<String> = mutableSetOf()

    fun init(name: String, exchanges: Set<String>) {
        this.name = name
        this.subscribedExchanges = exchanges.toMutableSet()
    }

    fun subscribe(exchangeName: String) {
        mwObj.setupSubscriberQueue(exchangeName)
        subscribedExchanges.add(exchangeName)
    }

    fun unsubscribe(exchangeName: String) {
        mwObj.unsubscribe(exchangeName)
        subscribedExchanges.remove(exchangeName)
    }

    fun changeSubscription(newExchangeName: String) {
        subscribedExchanges.forEach { unsubscribe(it) }
        subscribe(newExchangeName)
    }

    fun disconnect() {
        mwObj.disconnect()
    }

    override fun onMessageReceived(message: String) {
        logger.info("Message received: $message")
    }

    override fun onErrorOccurred(error: Throwable) {
        logger.error("Error occurred in SubMW", error)
    }

    fun dump() {
        try {
            logger.info("SubAppln::dump")
            logger.info("-Name: $name")
            logger.info("-Subscribed Exchanges: ${subscribedExchanges.joinToString(", ")}")
        } catch (e: Exception) {
            logger.error("Error during dump", e)
        }
    }
}

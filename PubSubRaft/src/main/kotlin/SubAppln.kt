package org.example

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlinx.cli.*

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

data class subCLIArgs(
    val name: String,
    val addr: String,
    val port: Int,
    val discovery: String,
    val numTopics: Int,
    val config: String,
    val frequency: Int,
    val iters: Int,
    val loglevel: Int
)

fun subParseCLIArgs(args: Array<String>): subCLIArgs {
    val parser = ArgParser("SubAppln")
    val name by parser.option(ArgType.String, shortName = "n", description = "Some name assigned to us. Keep it unique per subscriber").default("sub")
    val addr by parser.option(ArgType.String, shortName = "a", description = "IP addr of this subscriber to advertise").default("localhost")
    val port by parser.option(ArgType.Int, shortName = "p", description = "Port number on which our underlying subscriber ZMQ service runs").default(5570)
    val discovery by parser.option(ArgType.String, shortName = "d", description = "IP Addr:Port combo for the discovery service").default("localhost:5555")
    val numTopics by parser.option(ArgType.Int, shortName = "T", description = "Number of topics to subscribe to").default(7)
    val config by parser.option(ArgType.String, shortName = "c", description = "configuration file").default("config.ini")
    val frequency by parser.option(ArgType.Int, shortName = "f", description = "Rate at which topics are processed").default(1)
    val iters by parser.option(ArgType.Int, shortName = "i", description = "number of processing iterations").default(1000)
    val loglevel by parser.option(ArgType.Int, shortName = "l", description = "logging level").default(20)

    parser.parse(args)

    return subCLIArgs(name, addr, port, discovery, numTopics, config, frequency, iters, loglevel)
}

fun main(args: Array<String>) {
    val parsedArgs = subParseCLIArgs(args)
    val logger = LoggerFactory.getLogger("SubAppln")

    try {
        logger.info("Main - acquire a child logger and then log messages in the child")
        val subApp = SubAppln()
        val exchanges = setOf("python", "ruby", "typescript")
        subApp.init(parsedArgs.name, exchanges)
    } catch (e: Exception) {
        logger.error("Exception caught in main - ${e.message}", e)
    }
}

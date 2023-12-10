package org.example

import org.slf4j.LoggerFactory
import kotlinx.cli.*
import java.lang.Exception

data class ServiceInfo(val name: String, val address: String, val port: Int, var state: ServiceState)
data class ServiceState(val status: String, val additionalInfo: Map<String, String>)
data class LogEntry(val serviceName: String, val state: String, val additionalInfo: Map<String, String>)

class DiscoveryAppln : DiscoveryUpcallHandler {
    var logger = LoggerFactory.getLogger(DiscoveryAppln::class.java)
    private val discoveredServices = mutableSetOf<ServiceInfo>()
    private val mwObj = DiscoveryMW

    init {
        mwObj.setUpcallHandler(this)
    }

    override fun onServiceDiscovered(message: String) {
        logger.info("Service discovered: $message")
        val serviceInfo = parseServiceInfo(message)
        discoveredServices.add(serviceInfo)
    }

    override fun onLeaderElectionHandled() {
        logger.info("Leader election process handled")
    }

    fun discoverServices() {
        logger.info("Broadcasting service discovery request")
        mwObj.broadcastDiscoveryRequest(mapOf("request" to "discover_services"))
    }

    fun registerService(serviceInfo: ServiceInfo) {
        logger.info("Registering service: ${serviceInfo.name}")
        mwObj.broadcastDiscoveryRequest(mapOf("register_service" to serviceInfo.name))
        discoveredServices.add(serviceInfo)
    }

    private fun updateServiceState(serviceInfo: ServiceInfo, state: ServiceState) {
        logger.info("Updating state for service: ${serviceInfo.name}")
        val updatedServiceInfo = serviceInfo.copy(state = state)
        mwObj.broadcastDiscoveryRequest(mapOf("update_service_state" to updatedServiceInfo.name, "new_state" to state.status))
        discoveredServices.add(updatedServiceInfo)
    }

    fun handleLeaderElection() {
        logger.info("Handling leader election process")
        // Implement logic for starting or participating in leader election
        // This might involve calling a method in DiscoveryMW or another component
    }

    fun applyLogEntry(entry: LogEntry) {
        logger.info("Applying log entry to service state")
        val serviceToUpdate = discoveredServices.find { it.name == entry.serviceName }
        serviceToUpdate?.let {
            updateServiceState(it, ServiceState(entry.state, entry.additionalInfo))
        }
    }

    private fun parseServiceInfo(message: String): ServiceInfo {
        // Parse the message into a ServiceInfo object
        // Placeholder implementation, adapt it to match your message format
        return ServiceInfo("ServiceName", "Address", 8080, ServiceState("Status", mapOf()))
    }

    fun dump() {
        try {
            logger.info("DiscoveryAppln::dump")
            logger.info("-Discovered Services: ${discoveredServices.joinToString(", ") { service ->
                "${service.name} at ${service.address}:${service.port} [State: ${service.state.status}]"
            }}")
        } catch (e: Exception) {
            logger.error("Error during dump", e)
        }
    }
}

data class DiscoveryCLIArgs(
    val discoveryPort: Int,
    val loglevel: Int
)

fun discoveryParseCLIArgs(args: Array<String>): DiscoveryCLIArgs {
    val parser = ArgParser("DiscoveryAppln")
    val discoveryPort by parser.option(ArgType.Int, shortName = "p", description = "Port number for discovery service").default(5555)
    val loglevel by parser.option(ArgType.Int, shortName = "l", description = "logging level").default(20)
    parser.parse(args)
    return DiscoveryCLIArgs(discoveryPort, loglevel)
}

fun main(args: Array<String>) {
    val parsedArgs = discoveryParseCLIArgs(args)
    val logger = LoggerFactory.getLogger("DiscoveryAppln")

    try {
        logger.info("Main - acquire a child logger and then log messages in the child")
        // Initialize and configure the DiscoveryAppln
        val discoveryApp = DiscoveryAppln()

        logger.info("Discovery Service Port: ${parsedArgs.discoveryPort}")
        logger.info("Logging Level: ${parsedArgs.loglevel}")

        // Initialize Kafka with default local broker address
        DiscoveryMW.initKafka("localhost:9092")

        discoveryApp.discoverServices()
        val serviceInfo = ServiceInfo("ExampleService", "localhost", 8080, ServiceState("Active", emptyMap()))
        discoveryApp.registerService(serviceInfo)
        discoveryApp.handleLeaderElection()
        val logEntry = LogEntry("ExampleService", "Inactive", mapOf("reason" to "maintenance"))
        discoveryApp.applyLogEntry(logEntry)
        discoveryApp.dump()

    } catch (e: Exception) {
        logger.error("Exception caught in main - ${e.message}", e)
    }
}

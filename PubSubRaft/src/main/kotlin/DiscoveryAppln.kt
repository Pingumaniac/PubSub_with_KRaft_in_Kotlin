package org.example

import org.slf4j.LoggerFactory

data class ServiceInfo(val name: String, val address: String, val port: Int, var state: ServiceState)
data class ServiceState(val status: String, val additionalInfo: Map<String, String>)
data class LogEntry(val serviceName: String, val state: String, val additionalInfo: Map<String, String>)

class DiscoveryAppln : DiscoveryUpcallHandler {
    private val logger = LoggerFactory.getLogger(DiscoveryAppln::class.java)
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

    fun updateServiceState(serviceInfo: ServiceInfo, state: ServiceState) {
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

package org.example

import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.slf4j.LoggerFactory
import org.slf4j.Logger

class DiscoveryApplnTest {

    @Test
    fun testOnServiceDiscovered() {
        val mockLogger = mock(Logger::class.java)
        // Use LoggerFactory.getLogger to obtain a Logger instance
        `when`(LoggerFactory.getLogger(DiscoveryAppln::class.java)).thenReturn(mockLogger)
        val mockMwObj = mock(DiscoveryMW::class.java)
        `when`(mockMwObj.setUpcallHandler(any())).thenReturn(Unit)
        val discoveryAppln = DiscoveryAppln()
        val message = "Service discovered: ServiceName at Address:8080"
        discoveryAppln.onServiceDiscovered(message)
        // Verify info method called on Logger instance
        verify(mockLogger).info(message)
        val expectedServiceInfo = ServiceInfo("ServiceName", "Address", 8080, ServiceState("Status", emptyMap()))
        verify(mockMwObj).broadcastDiscoveryRequest(mapOf("register_service" to expectedServiceInfo.name))
    }

    @Test
    fun testOnLeaderElectionHandled() {
        val mockLogger = mock(Logger::class.java)
        `when`(LoggerFactory.getLogger(DiscoveryAppln::class.java)).thenReturn(mockLogger)
        val discoveryAppln = DiscoveryAppln()
        discoveryAppln.onLeaderElectionHandled()
        verify(mockLogger).info("Leader election process handled")
    }
    @Test
    fun testRegisterService() {
        val mockLogger = mock(Logger::class.java)
        // Correctly obtain a Logger instance
        `when`(LoggerFactory.getLogger(DiscoveryAppln::class.java)).thenReturn(mockLogger)
        val discoveryAppln = DiscoveryAppln()
        discoveryAppln.logger = mockLogger  // Assuming DiscoveryAppln has a public or internal logger field
        val serviceInfo = ServiceInfo("ExampleService", "localhost", 8080, ServiceState("Active", emptyMap()))
        discoveryAppln.registerService(serviceInfo)
        // Verify info method called on Logger instance
        verify(mockLogger).info("Registering service: ${serviceInfo.name}")
        verify(mockLogger).info("Updating state for service: ${serviceInfo.name}")
    }


    @Test
    fun testApplyLogEntry() {
        val mockLogger = mock(Logger::class.java)
        `when`(LoggerFactory.getLogger(DiscoveryAppln::class.java)).thenReturn(mockLogger)
        val discoveryAppln = DiscoveryAppln()
        discoveryAppln.logger = mockLogger  // Assuming DiscoveryAppln has a public or internal logger field
        val logEntry = LogEntry("ExampleService", "Inactive", mapOf("reason" to "maintenance"))
        discoveryAppln.applyLogEntry(logEntry)
        // Verify info method called on Logger instance
        verify(mockLogger).info("Applying log entry to service state")
    }

}
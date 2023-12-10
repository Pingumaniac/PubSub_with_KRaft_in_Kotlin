package org.example

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable

class PubApplnTest {

    private lateinit var pubAppln: PubAppln
    private val mockPubMW = mock(PubMW::class.java)

    @BeforeEach
    fun setUp() {
        pubAppln = PubAppln()
    }

    @Test
    fun testPublish() {
        val topic = "python"
        val messageContent = "flask"
        pubAppln.publish(topic, messageContent)
        verify(mockPubMW).publishMessage(serializeMessageToJson(topic, messageContent))
    }

    @Test
    fun testInitialize() {
        val name = "pubapp3"
        val numTopics = 3
        val topicList = listOf("python", "java", "c++")
        val iters = 10
        val frequency = 2
        pubAppln.initialize(name, numTopics, topicList, iters, frequency)
        assertEquals(name, pubAppln.name)
        assertEquals(numTopics, pubAppln.numTopics)
        assertEquals(topicList, pubAppln.topicList)
        assertEquals(iters, pubAppln.iters)
        assertEquals(frequency, pubAppln.frequency)
    }

    @Test
    fun testConfigurePublisher() {
        val host = "localhost"
        val port = 5672
        pubAppln.configurePublisher(mapOf("host" to host, "port" to port.toString()))
        verify(mockPubMW).connectToServer(host, port)
    }

    @Test
    fun testShutdownPublisher() {
        pubAppln.shutdownPublisher()
        verify(mockPubMW).closeConnection()
    }

    private fun serializeMessageToJson(topic: String, messageContent: String): String {
        return Json.encodeToString(Message(topic, messageContent))
    }

    @Serializable
    data class Message(val topic: String, val content: String)
}

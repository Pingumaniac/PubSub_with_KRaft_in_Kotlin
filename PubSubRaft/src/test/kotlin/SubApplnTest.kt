package org.example

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*

class SubApplnTest {

    private lateinit var subAppln: SubAppln
    private val mockSubMW = mock(SubMW::class.java)

    @BeforeEach
    fun setUp() {
        subAppln = SubAppln()
    }

    @Test
    fun testInitialize() {
        val name = "Test Subscriber"
        val exchanges = setOf("topic1", "topic2")
        subAppln.initialize(name, exchanges)
        assertEquals(name, subAppln.name)
        assertEquals(exchanges, subAppln.subscribedExchanges)
    }

    @Test
    fun testSubscribe() {
        val exchangeName = "python"
        subAppln.subscribe(exchangeName)
        verify(mockSubMW).setupSubscriberQueue(exchangeName)
        assertTrue(subAppln.subscribedExchanges.contains(exchangeName))
    }

    @Test
    fun testUnsubscribe() {
        val exchangeName = "python"
        subAppln.subscribedExchanges.add(exchangeName)
        subAppln.unsubscribe(exchangeName)
        verify(mockSubMW).unsubscribe(exchangeName)
        assertFalse(subAppln.subscribedExchanges.contains(exchangeName))
    }

    @Test
    fun testChangeSubscription() {
        val oldExchangeName = "F#"
        val newExchangeName = "F#"
        subAppln.subscribedExchanges.add(oldExchangeName)
        subAppln.changeSubscription(newExchangeName)
        verify(mockSubMW).unsubscribe(oldExchangeName)
        verify(mockSubMW).setupSubscriberQueue(newExchangeName)
        assertFalse(subAppln.subscribedExchanges.contains(oldExchangeName))
        assertTrue(subAppln.subscribedExchanges.contains(newExchangeName))
    }

    @Test
    fun testDisconnect() {
        subAppln.disconnect()
        verify(mockSubMW).disconnect()
    }

    @Test
    fun testOnMessageReceived() {
        val message = "{\"key\":\"value\"}"
        subAppln.onMessageReceived(message)
        val json = Json.parseToJsonElement(message)
        assertTrue(json is JsonObject, "Message should be a JSON object")
        val parsedMessage = json.toString()
        verify(subAppln.logger).info("JSON Message received: $parsedMessage")
    }

    @Test
    fun testOnMessageReceived_NonJson() {
        val message = "This is not JSON"
        subAppln.onMessageReceived(message)
        verify(subAppln.logger).info("Non-JSON message received: $message")
    }

    @Test
    fun testOnMessageReceived_SerializationError() {
        val message = "Invalid JSON"
        subAppln.onMessageReceived(message)
        val exception = mock(SerializationException::class.java)
        verify(subAppln.logger).error("Error deserializing message: $message", exception)
    }

    @Test
    fun testOnErrorOccurred() {
        val error = mock(Throwable::class.java)
        subAppln.onErrorOccurred(error)
        verify(subAppln.logger).error("Error occurred in SubMW", error)
    }
}
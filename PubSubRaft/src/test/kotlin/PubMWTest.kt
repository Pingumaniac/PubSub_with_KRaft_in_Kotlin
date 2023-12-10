package org.example

import com.fasterxml.jackson.databind.ObjectMapper
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*

class PubMWTest {

    @Test
    fun testConnectToServer() {
        val mockConnection = mock(Connection::class.java)
        val mockChannel = mock(Channel::class.java)
        val factory = mock(ConnectionFactory::class.java)
        `when`(factory.newConnection()).thenReturn(mockConnection)
        `when`(mockConnection.createChannel()).thenReturn(mockChannel)
        PubMW.connectToServer("localhost", 5672)
        verify(factory).host = "localhost"
        verify(factory).port = 5672
        verify(mockConnection).createChannel()
    }

    @Test
    fun testSetupMessageExchange() {
        val mockChannel = mock(Channel::class.java)
        PubMW.channel = mockChannel
        PubMW.setupMessageExchange("transpilation to c")
        verify(mockChannel).exchangeDeclare("transpilation to c", "fanout", true)
    }

    @Test
    fun testPublishMessage() {
        val mockChannel = mock(Channel::class.java)
        PubMW.channel = mockChannel
        PubMW.currentTopic = "python"
        val message = "transpilation to c"
        val serializedMessage = "transpilation to c"
        `when`(PubMW.serializeToJsonBytes(message)).thenReturn(serializedMessage.toByteArray())
        PubMW.publishMessage(message)
        verify(mockChannel).basicPublish("python", "", MessageProperties.PERSISTENT_TEXT_PLAIN, serializedMessage.toByteArray())
    }

    @Test
    fun testPublishMessage_NoTopic() {
        val mockHandler = mock(UpcallHandler::class.java)
        PubMW.setUpcallHandler(mockHandler)
        PubMW.publishMessage("transpilation to c")
        verify(mockHandler).onErrorOccurred(IllegalStateException("Exchange not initialized"))
    }

    @Test
    fun testSerializeToJsonBytes() {
        val message = "transpilation to c"
        val serializedMessage = "transpilation to c"
        val mockMapper = mock(ObjectMapper::class.java)
        PubMW.mapper = mockMapper
        `when`(mockMapper.writeValueAsBytes(message)).thenReturn(serializedMessage.toByteArray())
        val result = PubMW.serializeToJsonBytes(message)
        assertEquals(serializedMessage.toByteArray(), result)
    }
}
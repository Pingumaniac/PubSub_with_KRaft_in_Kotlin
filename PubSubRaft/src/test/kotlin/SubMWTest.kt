package org.example

import com.rabbitmq.client.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*

class SubMWTest {

    @Test
    fun testConnectToServer() {
        val mockConnection = mock(Connection::class.java)
        val mockChannel = mock(Channel::class.java)
        val factory = mock(ConnectionFactory::class.java)
        `when`(factory.newConnection()).thenReturn(mockConnection)
        `when`(mockConnection.createChannel()).thenReturn(mockChannel)
        SubMW.connectToServer("localhost", 5672)
        verify(factory).host = "localhost"
        verify(factory).port = 5672
        verify(mockConnection).createChannel()
    }

    @Test
    fun testSetupSubscriberQueue() {
        val mockChannel = mock(Channel::class.java)
        SubMW.channel = mockChannel
        // Mocking queueDeclare to return an instance of AMQP.Queue.DeclareOk
        `when`(mockChannel.queueDeclare()).thenReturn(mock(AMQP.Queue.DeclareOk::class.java))
        SubMW.setupSubscriberQueue("python")
        verify(mockChannel).queueDeclare()
        verify(mockChannel).queueBind(anyString(), eq("python"), eq(""))
        verify(mockChannel).basicConsume(anyString(), eq(true), any(DefaultConsumer::class.java))
    }


    @Test
    fun testHandleDelivery() {
        val mockHandler = mock(SubUpcallHandler::class.java)
        SubMW.setUpcallHandler(mockHandler)
        val mockConsumer = mock(DefaultConsumer::class.java)
        val message = "python"
        // Assuming you are deserializing to a String. Adjust as needed.
        `when`(SubMW.objectMapper.readValue(message, String::class.java)).thenReturn(message)
        mockConsumer.handleDelivery(
            "",
            mock(Envelope::class.java),
            mock(AMQP.BasicProperties::class.java),
            message.toByteArray()
        )
        verify(mockHandler).onMessageReceived(message)
    }

    @Test
    fun testHandleDelivery_SerializationError() {
        val mockHandler = mock(SubUpcallHandler::class.java)
        SubMW.setUpcallHandler(mockHandler)
        val mockConsumer = mock(DefaultConsumer::class.java)
        val message = "Invalid JSON"
        // Simulating a serialization error
        `when`(SubMW.objectMapper.readValue(message, String::class.java)).thenThrow(Exception("Serialization error"))
        mockConsumer.handleDelivery(
            "",
            mock(Envelope::class.java),
            mock(AMQP.BasicProperties::class.java),
            message.toByteArray()
        )
        // Capture the exception to verify
        val exceptionCaptor = ArgumentCaptor.forClass(Exception::class.java)
        verify(mockHandler).onErrorOccurred(exceptionCaptor.capture())
        assertEquals("Serialization error", exceptionCaptor.value.message)
    }


    @Test
    fun testUnsubscribe() {
        val mockChannel = mock(Channel::class.java)
        SubMW.channel = mockChannel
        SubMW.queueName = "python"
        SubMW.unsubscribe("")
        verify(mockChannel).queueUnbind("python", "", "")
    }

    @Test
    fun testChangeSubscription() {
        val mockChannel = mock(Channel::class.java)
        SubMW.channel = mockChannel
        SubMW.queueName = "python"
        SubMW.changeSubscription("javascript")
        verify(mockChannel).queueUnbind("python", "", "")
        verify(mockChannel).queueBind("python", "javascript", "")
    }

    @Test
    fun testDisconnect() {
        val mockChannel = mock(Channel::class.java)
        SubMW.channel = mockChannel
        SubMW.queueName = "python"
        SubMW.disconnect()
        verify(mockChannel).queueDelete("python")
        verify(mockChannel).close()
    }
}
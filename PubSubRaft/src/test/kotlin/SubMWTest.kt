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
        SubMW.setupSubscriberQueue("test-exchange")
        verify(mockChannel).queueDeclare()
        verify(mockChannel).queueBind(anyString(), eq("test-exchange"), eq(""))
        verify(mockChannel).basicConsume(anyString(), eq(true), any(DefaultConsumer::class.java))
    }


    @Test
    fun testHandleDelivery() {
        val mockHandler = mock(SubUpcallHandler::class.java)
        SubMW.setUpcallHandler(mockHandler)
        val mockConsumer = mock(DefaultConsumer::class.java)
        val message = "Test message"
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
        SubMW.queueName = "test-queue"
        SubMW.unsubscribe("")
        verify(mockChannel).queueUnbind("test-queue", "", "")
    }

    @Test
    fun testChangeSubscription() {
        val mockChannel = mock(Channel::class.java)
        SubMW.channel = mockChannel
        SubMW.queueName = "test-queue"
        SubMW.changeSubscription("new-exchange")
        verify(mockChannel).queueUnbind("test-queue", "", "")
        verify(mockChannel).queueBind("test-queue", "new-exchange", "")
    }

    @Test
    fun testDisconnect() {
        val mockChannel = mock(Channel::class.java)
        SubMW.channel = mockChannel
        SubMW.queueName = "test-queue"
        SubMW.disconnect()
        verify(mockChannel).queueDelete("test-queue")
        verify(mockChannel).close()
    }
}
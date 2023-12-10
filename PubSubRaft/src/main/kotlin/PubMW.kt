package org.example

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Channel
import com.rabbitmq.client.MessageProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

// Define an interface for handling upcall events
interface UpcallHandler {
    fun onMessagePublished(topic: String, message: String)
    fun onErrorOccurred(error: Throwable)
}

object PubMW {
    private var connection: Connection? = null
    var channel: Channel? = null
    var currentTopic: String? = null
    private var upcallHandler: UpcallHandler? = null

    // Initialize ObjectMapper for JSON serialization
    var mapper: ObjectMapper = jacksonObjectMapper().apply {
        registerKotlinModule()
    }

    fun connectToServer(host: String, port: Int) {
        val factory = ConnectionFactory()
        factory.host = host
        factory.port = port
        connection = factory.newConnection()
        channel = connection?.createChannel()
    }

    fun setupMessageExchange(exchangeName: String) {
        channel?.exchangeDeclare(exchangeName, "fanout", true)
        currentTopic = exchangeName
    }

    fun publishMessage(message: String) {
        try {
            val serializedMessage = serializeToJsonBytes(message)
            val properties = MessageProperties.PERSISTENT_TEXT_PLAIN

            currentTopic?.let {
                channel?.basicPublish(it, "", properties, serializedMessage)
                upcallHandler?.onMessagePublished(it, message)
            } ?: throw IllegalStateException("Exchange not initialized")
        } catch (e: Throwable) {
            upcallHandler?.onErrorOccurred(e)
        }
    }

    fun setUpcallHandler(handler: UpcallHandler) {
        upcallHandler = handler
    }

    fun closeConnection() {
        channel?.close()
        connection?.close()
    }

    fun serializeToJsonBytes(obj: Any): ByteArray {
        return mapper.writeValueAsBytes(obj)
    }
}

package org.example

import com.rabbitmq.client.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

interface SubUpcallHandler {
    fun onMessageReceived(message: String)
    fun onErrorOccurred(error: Throwable)
}

object SubMW {
    private var connection: Connection? = null
    private var channel: Channel? = null
    private var queueName: String? = null
    private var upcallHandler: SubUpcallHandler? = null

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    fun connectToServer(host: String, port: Int) {
        val factory = ConnectionFactory()
        factory.host = host
        factory.port = port
        connection = factory.newConnection()
        channel = connection?.createChannel()
    }

    fun setupSubscriberQueue(exchangeName: String) {
        queueName = channel?.queueDeclare()?.queue
        channel?.queueBind(queueName, exchangeName, "")

        val consumer = object : DefaultConsumer(channel) {
            override fun handleDelivery(
                consumerTag: String,
                envelope: Envelope,
                properties: AMQP.BasicProperties,
                body: ByteArray
            ) {
                try {
                    val message = String(body, Charsets.UTF_8)
                    val deserializedMessage: String = objectMapper.readValue(message)
                    upcallHandler?.onMessageReceived(deserializedMessage)
                } catch (e: Exception) {
                    upcallHandler?.onErrorOccurred(e)
                }
            }
        }

        channel?.basicConsume(queueName, true, consumer)
    }

    fun setUpcallHandler(handler: SubUpcallHandler) {
        upcallHandler = handler
    }

    fun unsubscribe(exchangeName: String) {
        if (channel != null && queueName != null) {
            channel?.queueUnbind(queueName, exchangeName, "")
        }
    }

    fun changeSubscription(newExchangeName: String) {
        unsubscribe("")
        setupSubscriberQueue(newExchangeName)
    }

    fun disconnect() {
        if (channel != null && queueName != null) {
            channel?.queueDelete(queueName)
            channel?.close()
        }
        connection?.close()
    }
}

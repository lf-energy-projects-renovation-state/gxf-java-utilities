// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message

import com.gxf.utilities.kafka.avro.AvroDeserializer
import com.gxf.utilities.kafka.avro.AvroSerializer
import java.util.UUID
import org.apache.avro.Schema
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.utils.KafkaTestUtils

object IntegrationTestHelper {
    fun createByteArrayKafkaConsumer(
        embeddedKafkaBroker: EmbeddedKafkaBroker,
        topic: String,
    ): Consumer<String, ByteArray> {
        val consumerFactory =
            DefaultKafkaConsumerFactory(
                KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), "true", embeddedKafkaBroker),
                StringDeserializer(),
                ByteArrayDeserializer(),
            )
        val consumer = consumerFactory.createConsumer()
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, topic)
        return consumer
    }

    fun createByteArrayKafkaProducer(
        embeddedKafkaBroker: EmbeddedKafkaBroker,
        messageSigningProducerProperties: Map<String, Any>,
    ): Producer<String, ByteArray> {
        val producerProps: Map<String, Any> =
            producerProps(embeddedKafkaBroker.brokersAsString) + messageSigningProducerProperties
        val producerFactory = DefaultKafkaProducerFactory(producerProps, StringSerializer(), ByteArraySerializer())
        return producerFactory.createProducer()
    }

    fun createAvroKafkaConsumer(
        embeddedKafkaBroker: EmbeddedKafkaBroker,
        topic: String,
    ): Consumer<String, SpecificRecordBase> {
        val consumerFactory =
            DefaultKafkaConsumerFactory(
                KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), "true", embeddedKafkaBroker),
                StringDeserializer(),
                AvroDeserializer(listOf(Message.getClassSchema())),
            )
        val consumer = consumerFactory.createConsumer()
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, topic)
        return consumer
    }

    fun createAvroKafkaProducer(
        embeddedKafkaBroker: EmbeddedKafkaBroker,
        messageSigningProducerProperties: Map<String, Any>,
    ): Producer<String, SpecificRecordBase> {
        val producerProps: Map<String, Any> =
            producerProps(embeddedKafkaBroker.brokersAsString) + messageSigningProducerProperties
        val producerFactory = DefaultKafkaProducerFactory(producerProps, StringSerializer(), AvroSerializer())
        return producerFactory.createProducer()
    }

    private fun producerProps(brokers: String): Map<String, Any> {
        return mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to brokers,
            ProducerConfig.BATCH_SIZE_CONFIG to "16384",
            ProducerConfig.LINGER_MS_CONFIG to 1,
            ProducerConfig.BUFFER_MEMORY_CONFIG to "33554432",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to AvroSerializer::class.java,
        )
    }
}

class Message(private var message: String?) : SpecificRecordBase() {
    constructor() : this(null) {}

    companion object {
        fun getClassSchema(): Schema =
            Schema.Parser()
                .parse(
                    """{"type":"record","name":"Message","namespace":"com.gxf.utilities.kafka.message","fields":[{"name":"message","type":{"type":"string","avro.java.string":"String"}}]}"""
                )
    }

    override fun getSchema() = getClassSchema()

    override fun get(field: Int): Any {
        return message!!
    }

    override fun put(field: Int, value: Any) {
        message = value.toString()
    }
}

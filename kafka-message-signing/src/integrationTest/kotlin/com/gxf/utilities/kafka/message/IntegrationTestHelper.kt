// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message

import com.gxf.utilities.kafka.message.signing.MessageSigner
import com.gxf.utilities.kafka.message.signing.MessageSigningProducerInterceptor
import java.util.UUID
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
    fun createKafkaConsumer(embeddedKafkaBroker: EmbeddedKafkaBroker, topic: String): Consumer<String, ByteArray> {
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

    fun createKafkaProducer(
        embeddedKafkaBroker: EmbeddedKafkaBroker,
        messageSigner: MessageSigner,
    ): Producer<String, ByteArray> {
        val producerProps: Map<String, Any> = HashMap(producerProps(embeddedKafkaBroker.brokersAsString, messageSigner))
        val producerFactory = DefaultKafkaProducerFactory(producerProps, StringSerializer(), ByteArraySerializer())
        return producerFactory.createProducer()
    }

    private fun producerProps(brokers: String, messageSigner: MessageSigner): Map<String, Any> {
        return mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to brokers,
            ProducerConfig.BATCH_SIZE_CONFIG to "16384",
            ProducerConfig.LINGER_MS_CONFIG to 1,
            ProducerConfig.BUFFER_MEMORY_CONFIG to "33554432",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to ByteArraySerializer::class.java,
            ProducerConfig.INTERCEPTOR_CLASSES_CONFIG to MessageSigningProducerInterceptor::class.java.name,
            "message.signer" to messageSigner,
        )
    }
}

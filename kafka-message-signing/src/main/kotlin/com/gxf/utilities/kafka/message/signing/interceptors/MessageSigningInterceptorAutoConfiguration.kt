// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing.interceptors

import com.gxf.utilities.kafka.message.signing.MessageSigner
import org.apache.kafka.clients.producer.ProducerConfig
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ConditionalOnProperty(value = ["message-signing.use-interceptor"], havingValue = "true", matchIfMissing = false)
@Configuration
class MessageSigningInterceptorAutoConfiguration {
    @Bean
    fun producerPropertiesForByteArrayRecords(
        kafkaProperties: KafkaProperties,
        messageSigner: MessageSigner,
    ): Map<String, Any> {
        val properties = kafkaProperties.buildProducerProperties()
        properties[ProducerConfig.INTERCEPTOR_CLASSES_CONFIG] =
            listOf(MessageSigningByteArrayProducerInterceptor::class.java)
        properties["message.signer"] = messageSigner
        return properties
    }

    @Bean
    fun producerPropertiesForAvroRecords(
        kafkaProperties: KafkaProperties,
        messageSigner: MessageSigner,
    ): Map<String, Any> {
        val properties = kafkaProperties.buildProducerProperties()
        properties[ProducerConfig.INTERCEPTOR_CLASSES_CONFIG] =
            listOf(MessageSigningAvroProducerInterceptor::class.java)
        properties["message.signer"] = messageSigner
        return properties
    }
}

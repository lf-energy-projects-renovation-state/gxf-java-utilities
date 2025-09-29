// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing.interceptors

import com.gxf.utilities.kafka.message.signing.MessageSigner
import org.apache.kafka.clients.producer.ProducerConfig
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.ssl.SslBundles
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MessageSigningInterceptorAutoConfiguration(private val sslBundles: SslBundles) {
    @Bean
    fun producerPropertiesForByteArrayRecords(
        kafkaProperties: KafkaProperties,
        messageSigner: MessageSigner,
    ): Map<String, Any> {
        val properties = kafkaProperties.buildProducerProperties(sslBundles)
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
        val properties = kafkaProperties.buildProducerProperties(sslBundles)
        properties[ProducerConfig.INTERCEPTOR_CLASSES_CONFIG] =
            listOf(MessageSigningAvroProducerInterceptor::class.java)
        properties["message.signer"] = messageSigner
        return properties
    }
}

// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing

import java.util.function.Consumer
import org.apache.avro.Schema
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Header
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Only tests the `...UsingHeader` methods */
class MessageSignerTestUsingHeader {
    private val messageSignerProperties = TestConstants.messageSignerProperties

    private val messageSigner = MessageSigner(messageSignerProperties)

    @Test
    fun signsRecordHeaderWithoutSignature() {
        val record: ProducerRecord<String, Message> = producerRecord()

        // Assert that the returned var is of exactly the same type as the input
        val sameTypeResult: ProducerRecord<String, Message> = messageSigner.signUsingHeader(record)

        assertThat(record.headers().lastHeader(MessageSigner.RECORD_HEADER_KEY_SIGNATURE)).isNotNull()
    }

    @Test
    fun signsRecordHeaderReplacingSignature() {
        val randomSignature = TestConstants.randomSignature()
        val record = producerRecord()
        record.headers().add(MessageSigner.RECORD_HEADER_KEY_SIGNATURE, randomSignature.array())

        val actualSignatureBefore = record.headers().lastHeader(MessageSigner.RECORD_HEADER_KEY_SIGNATURE).value()
        assertThat(actualSignatureBefore).isNotNull().isEqualTo(randomSignature.array())

        messageSigner.signUsingHeader(record)

        val actualSignatureAfter = record.headers().lastHeader(MessageSigner.RECORD_HEADER_KEY_SIGNATURE).value()
        assertThat(actualSignatureAfter).isNotNull().isNotEqualTo(randomSignature.array())
    }

    @Test
    fun verifiesRecordsWithValidSignature() {
        val signedRecord = properlySignedRecord()

        val result = messageSigner.verifyUsingHeader(signedRecord)

        assertThat(result).isTrue()
    }

    @Test
    fun doesNotVerifyRecordsWithoutSignature() {
        val consumerRecord = consumerRecord()

        val validSignature = messageSigner.verifyUsingHeader(consumerRecord)

        assertThat(validSignature).isFalse()
    }

    @Test
    fun doesNotVerifyRecordsWithIncorrectSignature() {
        val consumerRecord = consumerRecord()
        val randomSignature = TestConstants.randomSignature()
        consumerRecord.headers().add(MessageSigner.RECORD_HEADER_KEY_SIGNATURE, randomSignature.array())

        val validSignature = messageSigner.verifyUsingHeader(consumerRecord)

        assertThat(validSignature).isFalse()
    }

    private fun <K, V> producerRecordToConsumerRecord(producerRecord: ProducerRecord<K, V>): ConsumerRecord<K, V> {
        val consumerRecord =
            ConsumerRecord(producerRecord.topic(), 0, 123L, producerRecord.key(), producerRecord.value())
        producerRecord.headers().forEach(Consumer { header: Header? -> consumerRecord.headers().add(header) })
        return consumerRecord
    }

    private fun properlySignedRecord(): ConsumerRecord<String, Message> {
        val producerRecord = producerRecord()
        messageSigner.signUsingHeader(producerRecord)
        return producerRecordToConsumerRecord(producerRecord)
    }

    private fun producerRecord(): ProducerRecord<String, Message> {
        return ProducerRecord("topic", message())
    }

    private fun consumerRecord(): ConsumerRecord<String, Message> {
        return ConsumerRecord("topic", 0, 123L, null, message())
    }

    private fun message(): Message {
        return Message("super special message")
    }

    internal class Message(private var message: String?) : SpecificRecordBase() {

        override fun getSchema(): Schema {
            return Schema.Parser()
                .parse(
                    """{"type":"record","name":"Message","namespace":"com.alliander.osgp.kafka.message.signing","fields":[{"name":"message","type":{"type":"string","avro.java.string":"String"}}]}"""
                )
        }

        override fun get(field: Int): Any {
            return message!!
        }

        override fun put(field: Int, value: Any) {
            message = value.toString()
        }
    }
}

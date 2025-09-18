// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing

import com.gxf.utilities.kafka.message.signing.TestHelper.producerRecordToConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MessageSignerTestByteArrayUsingHeader {
    private val messageSignerProperties = TestConstants.messageSignerProperties.apply { stripAvroHeader = false }
    private val messageSigner = MessageSigner(messageSignerProperties)

    @Test
    fun signsRecordHeaderWithoutSignature() {
        val record: ProducerRecord<String, ByteArray> = producerRecordByteArray()

        // Assert that the returned var is of exactly the same type as the input
        val sameTypeResult: ProducerRecord<String, ByteArray> = messageSigner.signByteArrayRecordUsingHeader(record)

        assertThat(record.headers().lastHeader(MessageSigner.RECORD_HEADER_KEY_SIGNATURE)).isNotNull()
    }

    @Test
    fun signsRecordHeaderReplacingSignature() {
        val randomSignature = TestConstants.randomSignature()
        val record = producerRecordByteArray()
        record.headers().add(MessageSigner.RECORD_HEADER_KEY_SIGNATURE, randomSignature.array())

        val actualSignatureBefore = record.headers().lastHeader(MessageSigner.RECORD_HEADER_KEY_SIGNATURE).value()
        assertThat(actualSignatureBefore).isNotNull().isEqualTo(randomSignature.array())

        messageSigner.signByteArrayRecordUsingHeader(record)

        val actualSignatureAfter = record.headers().lastHeader(MessageSigner.RECORD_HEADER_KEY_SIGNATURE).value()
        assertThat(actualSignatureAfter).isNotNull().isNotEqualTo(randomSignature.array())
    }

    @Test
    fun verifiesRecordsWithValidSignature() {
        val signedRecord = properlySignedByteArrayRecord()

        val result = messageSigner.verifyByteArrayRecordUsingHeader(signedRecord)

        assertThat(result).isTrue()
    }

    @Test
    fun doesNotVerifyRecordsWithoutSignature() {
        val consumerRecord = consumerRecordByteArray()

        val validSignature = messageSigner.verifyByteArrayRecordUsingHeader(consumerRecord)

        assertThat(validSignature).isFalse()
    }

    @Test
    fun doesNotVerifyRecordsWithInvalidSignature() {
        val consumerRecord = consumerRecordByteArray()
        val randomSignature = TestConstants.randomSignature()
        consumerRecord.headers().add(MessageSigner.RECORD_HEADER_KEY_SIGNATURE, randomSignature.array())

        val validSignature = messageSigner.verifyByteArrayRecordUsingHeader(consumerRecord)

        assertThat(validSignature).isFalse()
    }

    private fun producerRecordByteArray(): ProducerRecord<String, ByteArray> {
        val value = "Test message".toByteArray()
        return ProducerRecord("test-topic", "key1", value)
    }

    private fun properlySignedByteArrayRecord(): ConsumerRecord<String, ByteArray> {
        val producerRecord = producerRecordByteArray()
        messageSigner.signByteArrayRecordUsingHeader(producerRecord)
        return producerRecordToConsumerRecord(producerRecord)
    }

    private fun consumerRecordByteArray(): ConsumerRecord<String, ByteArray> {
        val value = "Test message".toByteArray()
        return ConsumerRecord("test-topic", 0, 0L, "key1", value)
    }
}

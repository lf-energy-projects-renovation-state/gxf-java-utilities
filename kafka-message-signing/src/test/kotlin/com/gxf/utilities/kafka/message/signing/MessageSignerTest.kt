// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing

import com.gxf.utilities.kafka.message.wrapper.SignableMessageWrapper
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Random
import java.util.function.Consumer
import org.apache.avro.Schema
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Header
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class MessageSignerTest {

    private val messageSignerProperties =
        MessageSigningProperties(
            signingEnabled = true,
            stripAvroHeader = true,
            signatureAlgorithm = "SHA256withRSA",
            signatureProvider = "SunRsaSign",
            keyAlgorithm = "RSA",
            privateKeyFile = ClassPathResource("/rsa-private.pem"),
            publicKeyFile = ClassPathResource("/rsa-public.pem"),
        )

    private val messageSigner = MessageSigner(messageSignerProperties)

    @Test
    fun signsMessageWithoutSignature() {
        val messageWrapper: SignableMessageWrapper<*> = this.messageWrapper()

        messageSigner.signUsingField(messageWrapper)

        assertThat(messageWrapper.getSignature()).isNotNull()
    }

    @Test
    fun signsRecordHeaderWithoutSignature() {
        val record = this.producerRecord()

        messageSigner.signUsingHeader(record)

        assertThat(record.headers().lastHeader(MessageSigner.RECORD_HEADER_KEY_SIGNATURE)).isNotNull()
    }

    @Test
    fun signsMessageReplacingSignature() {
        val randomSignature = this.randomSignature()
        val messageWrapper = this.messageWrapper()
        messageWrapper.setSignature(randomSignature)

        val actualSignatureBefore = messageWrapper.getSignature()
        assertThat(actualSignatureBefore).isNotNull().isEqualTo(randomSignature)

        messageSigner.signUsingField(messageWrapper)

        val actualSignatureAfter = messageWrapper.getSignature()
        assertThat(actualSignatureAfter).isNotNull().isNotEqualTo(randomSignature)
    }

    @Test
    fun signsRecordHeaderReplacingSignature() {
        val randomSignature = this.randomSignature()
        val record = this.producerRecord()
        record.headers().add(MessageSigner.RECORD_HEADER_KEY_SIGNATURE, randomSignature.array())

        val actualSignatureBefore = record.headers().lastHeader(MessageSigner.RECORD_HEADER_KEY_SIGNATURE).value()
        assertThat(actualSignatureBefore).isNotNull().isEqualTo(randomSignature.array())

        messageSigner.signUsingHeader(record)

        val actualSignatureAfter = record.headers().lastHeader(MessageSigner.RECORD_HEADER_KEY_SIGNATURE).value()
        assertThat(actualSignatureAfter).isNotNull().isNotEqualTo(randomSignature)
    }

    @Test
    fun verifiesMessagesWithValidSignature() {
        val message = this.properlySignedMessage()

        val signatureWasVerified = messageSigner.verifyUsingField(message)

        assertThat(signatureWasVerified).isTrue()
    }

    @Test
    fun verifiesRecordsWithValidSignature() {
        val signedRecord = this.properlySignedRecord()

        val result = messageSigner.verifyUsingHeader(signedRecord)

        assertThat(result).isTrue()
    }

    @Test
    fun doesNotVerifyMessagesWithoutSignature() {
        val messageWrapper = this.messageWrapper()

        val validSignature = messageSigner.verifyUsingField(messageWrapper)

        assertThat(validSignature).isFalse()
    }

    @Test
    fun doesNotVerifyRecordsWithoutSignature() {
        val consumerRecord = this.consumerRecord()

        val validSignature = messageSigner.verifyUsingHeader(consumerRecord)

        assertThat(validSignature).isFalse()
    }

    @Test
    fun doesNotVerifyMessagesWithIncorrectSignature() {
        val randomSignature = this.randomSignature()
        val messageWrapper = this.messageWrapper(randomSignature)

        val validSignature = messageSigner.verifyUsingField(messageWrapper)

        assertThat(validSignature).isFalse()
    }

    @Test
    fun doesNotVerifyRecordsWithIncorrectSignature() {
        val consumerRecord = this.consumerRecord()
        val randomSignature = this.randomSignature()
        consumerRecord.headers().add(MessageSigner.RECORD_HEADER_KEY_SIGNATURE, randomSignature.array())

        val validSignature = messageSigner.verifyUsingHeader(consumerRecord)

        assertThat(validSignature).isFalse()
    }

    @Test
    fun verifiesMessagesPreservingTheSignatureAndItsProperties() {
        val message = this.properlySignedMessage()
        val originalSignature = message.getSignature()

        messageSigner.verifyUsingField(message)

        val verifiedSignature = message.getSignature()
        assertThat(verifiedSignature).isEqualTo(originalSignature)
    }

    @Test
    fun signingCanBeDisabled() {
        val signingDisabledProperties = MessageSigningProperties(signingEnabled = false)
        val messageSignerSigningDisabled = MessageSigner(signingDisabledProperties)

        assertThat(messageSignerSigningDisabled.canSignMessages()).isFalse()
        assertThat(messageSignerSigningDisabled.canVerifyMessageSignatures()).isFalse()
    }

    private fun messageWrapper(): TestableWrapper {
        return TestableWrapper()
    }

    private fun messageWrapper(signature: ByteBuffer): TestableWrapper {
        val testableWrapper = TestableWrapper()
        testableWrapper.setSignature(signature)
        return testableWrapper
    }

    private fun properlySignedMessage(): TestableWrapper {
        val messageWrapper = this.messageWrapper()
        messageSigner.signUsingField(messageWrapper)
        return messageWrapper
    }

    private fun properlySignedRecord(): ConsumerRecord<String, Message> {
        val producerRecord = this.producerRecord()
        messageSigner.signUsingHeader(producerRecord)
        return this.producerRecordToConsumerRecord(producerRecord)
    }

    private fun <K, V> producerRecordToConsumerRecord(producerRecord: ProducerRecord<K, V>): ConsumerRecord<K, V> {
        val consumerRecord =
            ConsumerRecord(producerRecord.topic(), 0, 123L, producerRecord.key(), producerRecord.value())
        producerRecord.headers().forEach(Consumer { header: Header? -> consumerRecord.headers().add(header) })
        return consumerRecord
    }

    private fun randomSignature(): ByteBuffer {
        val random: Random = SecureRandom()
        val keySize = 2048

        val signature = ByteArray(keySize / 8)
        random.nextBytes(signature)

        return ByteBuffer.wrap(signature)
    }

    private fun producerRecord(): ProducerRecord<String, Message> {
        return ProducerRecord("topic", this.message())
    }

    private fun consumerRecord(): ConsumerRecord<String, Message> {
        return ConsumerRecord("topic", 0, 123L, null, this.message())
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
            this.message = value.toString()
        }
    }

    private class TestableWrapper : SignableMessageWrapper<String>("Some test message") {
        private var signature: ByteBuffer? = null

        override fun toByteBuffer(): ByteBuffer {
            return ByteBuffer.wrap(message.toByteArray(StandardCharsets.UTF_8))
        }

        override fun getSignature(): ByteBuffer? {
            return this.signature
        }

        override fun setSignature(signature: ByteBuffer?) {
            this.signature = signature
        }
    }
}

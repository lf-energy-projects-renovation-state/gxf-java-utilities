// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package com.gxf.utilities.kafka.message.signing

import com.gxf.utilities.kafka.message.wrapper.SignableMessageWrapper
import org.apache.avro.Schema
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Header
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*
import java.util.function.Consumer

class MessageSignerTest {

    private val messageSignerProperties = MessageSigningProperties(
        signingEnabled = true,
        stripAvroHeader = true,
        algorithm = "SHA256withRSA",
        provider = "SunRsaSign",
        keyAlgorithm = "RSA",
        privateKeyFile = ClassPathResource("/rsa-private.pem"),
        publicKeyFile = ClassPathResource("/rsa-public.pem")
    )

    private val messageSigner = MessageSigner(messageSignerProperties)

    @Test
    fun signsMessageWithoutSignature() {
        val messageWrapper: SignableMessageWrapper<*> = this.messageWrapper()

        println(messageSigner.toString())
        messageSigner.sign(messageWrapper)

        assertThat(messageWrapper.getSignature()).isNotNull()
    }

    @Test
    fun signsRecordHeaderWithoutSignature() {
        val record = this.producerRecord()

        messageSigner.sign(record)

        assertThat(record.headers().lastHeader(MessageSigner.RECORD_HEADER_KEY_SIGNATURE)).isNotNull()
    }

    @Test
    fun signsMessageReplacingSignature() {
        val randomSignature = this.randomSignature()
        val messageWrapper = this.messageWrapper()
        messageWrapper.setSignature(ByteBuffer.wrap(randomSignature))

        val actualSignatureBefore = this.bytes(messageWrapper.getSignature())
        assertThat(actualSignatureBefore).isNotNull().isEqualTo(randomSignature)

        messageSigner.sign(messageWrapper)

        val actualSignatureAfter = this.bytes(messageWrapper.getSignature())
        assertThat(actualSignatureAfter).isNotNull().isNotEqualTo(randomSignature)
    }

    @Test
    fun signsRecordHeaderReplacingSignature() {
        val randomSignature = this.randomSignature()
        val record = this.producerRecord()
        record.headers().add(MessageSigner.RECORD_HEADER_KEY_SIGNATURE, randomSignature)

        val actualSignatureBefore = record.headers().lastHeader(MessageSigner.RECORD_HEADER_KEY_SIGNATURE).value()
        assertThat(actualSignatureBefore).isNotNull().isEqualTo(randomSignature)

        messageSigner.sign(record)

        val actualSignatureAfter = record.headers().lastHeader(MessageSigner.RECORD_HEADER_KEY_SIGNATURE).value()
        assertThat(actualSignatureAfter).isNotNull().isNotEqualTo(randomSignature)
    }

    @Test
    fun verifiesMessagesWithValidSignature() {
        val message = this.properlySignedMessage()

        val signatureWasVerified = messageSigner.verify(message)

        assertThat(signatureWasVerified).isTrue()
    }

    @Test
    fun verifiesRecordsWithValidSignature() {
        val signedRecord = this.properlySignedRecord()

        val signatureWasVerified: Boolean = messageSigner.verify(signedRecord)

        assertThat(signatureWasVerified).isTrue()
    }

    @Test
    fun doesNotVerifyMessagesWithoutSignature() {
        val messageWrapper = this.messageWrapper()

        val signatureWasVerified = messageSigner.verify(messageWrapper)

        assertThat(signatureWasVerified).isFalse()
    }

    @Test
    fun doesNotVerifyRecordsWithoutSignature() {
        val expectedMessage = "This ProducerRecord does not contain a signature header"
        val consumerRecord = this.consumerRecord()

        val exception: Exception = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException::class.java
        ) {
            messageSigner.verify(
                consumerRecord
            )
        }
        val actualMessage = exception.message

        org.junit.jupiter.api.Assertions.assertTrue(actualMessage!!.contains(expectedMessage))
    }

    @Test
    fun doesNotVerifyMessagesWithIncorrectSignature() {
        val randomSignature = this.randomSignature()
        val messageWrapper = this.messageWrapper(randomSignature)

        val signatureWasVerified = messageSigner.verify(messageWrapper)

        assertThat(signatureWasVerified).isFalse()
    }

    @Test
    fun verifiesMessagesPreservingTheSignatureAndItsProperties() {
        val message = this.properlySignedMessage()
        val originalSignature = message.getSignature()
        val originalPosition = originalSignature!!.position()
        val originalLimit = originalSignature.limit()
        val originalRemaining = originalSignature.remaining()

        messageSigner.verify(message)

        val verifiedSignature = message.getSignature()
        assertThat(verifiedSignature).isEqualTo(originalSignature)
        assertThat(verifiedSignature!!.position()).isEqualTo(originalPosition)
        assertThat(verifiedSignature.limit()).isEqualTo(originalLimit)
        assertThat(verifiedSignature.remaining()).isEqualTo(originalRemaining)
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

    private fun messageWrapper(signature: ByteArray): TestableWrapper {
        val testableWrapper = TestableWrapper()
        testableWrapper.setSignature(ByteBuffer.wrap(signature))
        return testableWrapper
    }

    private fun properlySignedMessage(): TestableWrapper {
        val messageWrapper = this.messageWrapper()
        messageSigner.sign(messageWrapper)
        return messageWrapper
    }

    private fun properlySignedRecord(): ConsumerRecord<String, Message> {
        val producerRecord = this.producerRecord()
        messageSigner.sign(producerRecord)
        return this.producerRecordToConsumerRecord(producerRecord)
    }

    private fun <K, V> producerRecordToConsumerRecord(producerRecord: ProducerRecord<K, V>): ConsumerRecord<K, V> {
        val consumerRecord =
            ConsumerRecord(producerRecord.topic(), 0, 123L, producerRecord.key(), producerRecord.value())
        producerRecord.headers().forEach(Consumer { header: Header? ->
            consumerRecord.headers().add(header)
        })
        return consumerRecord
    }

    private fun randomSignature(): ByteArray {
        val random: Random = SecureRandom()
        val keySize = 2048

        val signature = ByteArray(keySize / 8)
        random.nextBytes(signature)

        return signature
    }

    private fun bytes(byteBuffer: ByteBuffer?): ByteArray? {
        if (byteBuffer == null) {
            return null
        }
        val bytes = ByteArray(byteBuffer.remaining())
        byteBuffer[bytes]
        return bytes
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
                .parse("""{"type":"record","name":"Message","namespace":"com.alliander.osgp.kafka.message.signing","fields":[{"name":"message","type":{"type":"string","avro.java.string":"String"}}]}""")
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

        override fun toByteBuffer(): ByteBuffer? {
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

// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing

import com.gxf.utilities.kafka.message.wrapper.SignableMessageWrapper
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Random
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Only tests the now deprecated SignableMessageWrapper methods */
class DeprecatedMessageSignerTest {

    private val messageSignerProperties = TestConstants.messageSignerProperties

    private val messageSigner = MessageSigner(messageSignerProperties)

    @Test
    fun signsMessageWithoutSignature() {
        val messageWrapper: SignableMessageWrapper<*> = this.messageWrapper()

        messageSigner.signUsingField(messageWrapper)

        assertThat(messageWrapper.getSignature()).isNotNull()
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
    fun verifiesMessagesWithValidSignature() {
        val message = this.properlySignedMessage()

        val signatureWasVerified = messageSigner.verifyUsingField(message)

        assertThat(signatureWasVerified).isTrue()
    }

    @Test
    fun doesNotVerifyMessagesWithoutSignature() {
        val messageWrapper = this.messageWrapper()

        val validSignature = messageSigner.verifyUsingField(messageWrapper)

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
    fun verifiesMessagesPreservingTheSignatureAndItsProperties() {
        val message = this.properlySignedMessage()
        val originalSignature = message.getSignature()

        messageSigner.verifyUsingField(message)

        val verifiedSignature = message.getSignature()
        assertThat(verifiedSignature).isEqualTo(originalSignature)
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

    private fun randomSignature(): ByteBuffer {
        val random: Random = SecureRandom()
        val keySize = 2048

        val signature = ByteArray(keySize / 8)
        random.nextBytes(signature)

        return ByteBuffer.wrap(signature)
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

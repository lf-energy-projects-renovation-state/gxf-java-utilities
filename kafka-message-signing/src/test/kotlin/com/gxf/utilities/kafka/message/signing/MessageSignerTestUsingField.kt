// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing

import com.gxf.utilities.kafka.message.wrapper.FlexibleSignableMessageWrapper
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Random
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Only tests the `...UsingField` methods */
class MessageSignerTestUsingField {
    private val messageSignerProperties = TestConstants.messageSignerProperties

    private val messageSigner = MessageSigner(messageSignerProperties)

    @Test
    fun signsMessageWithoutSignature() {
        val messageWrapper = messageWrapper()

        messageSigner.signUsingField(messageWrapper)

        assertThat(messageWrapper.getSignature()).isNotNull()
    }

    @Test
    fun signsMessageReplacingSignature() {
        val randomSignature = randomSignature()
        val messageWrapper = messageWrapper()
        messageWrapper.setSignature(randomSignature)

        val actualSignatureBefore = messageWrapper.getSignature()
        assertThat(actualSignatureBefore).isNotNull().isEqualTo(randomSignature)

        messageSigner.signUsingField(messageWrapper)

        val actualSignatureAfter = messageWrapper.getSignature()
        assertThat(actualSignatureAfter).isNotNull().isNotEqualTo(randomSignature)
    }

    @Test
    fun verifiesMessagesWithValidSignature() {
        val message = properlySignedMessage()

        val signatureWasVerified = messageSigner.verifyUsingField(message)

        assertThat(signatureWasVerified).isTrue()
    }

    @Test
    fun doesNotVerifyMessagesWithoutSignature() {
        val messageWrapper = messageWrapper()

        val validSignature = messageSigner.verifyUsingField(messageWrapper)

        assertThat(validSignature).isFalse()
    }

    @Test
    fun doesNotVerifyMessagesWithIncorrectSignature() {
        val randomSignature = randomSignature()
        val messageWrapper = messageWrapper(randomSignature)

        val validSignature = messageSigner.verifyUsingField(messageWrapper)

        assertThat(validSignature).isFalse()
    }

    @Test
    fun verifiesMessagesPreservingTheSignatureAndItsProperties() {
        val message = properlySignedMessage()
        val originalSignature = message.getSignature()

        messageSigner.verifyUsingField(message)

        val verifiedSignature = message.getSignature()
        assertThat(verifiedSignature).isEqualTo(originalSignature)
    }

    private fun messageWrapper(): FlexibleSignableMessageWrapper<MessageWithSignature> {
        return FlexibleSignableMessageWrapper(
            message(),
            messageGetter = { ByteBuffer.wrap(it.payload.toByteArray()) },
            signatureGetter = { it.signature ?: ByteBuffer.allocate(0) },
            signatureSetter = { message, signature -> message.signature = signature },
        )
    }

    private fun messageWrapper(signature: ByteBuffer): FlexibleSignableMessageWrapper<MessageWithSignature> {
        val testableWrapper = messageWrapper()
        testableWrapper.setSignature(signature)
        return testableWrapper
    }

    private fun properlySignedMessage(): FlexibleSignableMessageWrapper<MessageWithSignature> {
        val messageWrapper = messageWrapper()
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

    private fun message(): MessageWithSignature {
        return MessageWithSignature("super special message")
    }

    internal class MessageWithSignature(var payload: String, var signature: ByteBuffer? = null)
}

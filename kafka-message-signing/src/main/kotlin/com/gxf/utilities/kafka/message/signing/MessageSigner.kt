// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing

import com.gxf.utilities.kafka.avro.AvroEncoder
import com.gxf.utilities.kafka.message.wrapper.FlexibleSignableMessageWrapper
import com.gxf.utilities.kafka.message.wrapper.SignableMessageWrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.regex.Pattern
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.ssl.pem.PemContent
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
// Only instantiate when no other bean has been configured
@ConditionalOnMissingBean(MessageSigner::class)
class MessageSigner(properties: MessageSigningProperties) {
    val signingEnabled: Boolean = properties.signingEnabled
    private val stripAvroHeader: Boolean = properties.stripAvroHeader

    private val signatureAlgorithm: String = properties.signatureAlgorithm
    private val signatureProvider: String? = properties.signatureProvider
    private val keyAlgorithm: String = properties.keyAlgorithm

    private var signingKey: PrivateKey? = readPrivateKey(properties.privateKeyFile)
    private var verificationKey: PublicKey? = readPublicKey(keyAlgorithm, properties.publicKeyFile)
    private var previousVerificationKey: PublicKey? = readPublicKey(keyAlgorithm, properties.previousPublicKeyFile)

    init {
        if (properties.signingEnabled) {
            require(!(signingKey == null && verificationKey == null)) {
                "A signing key (PrivateKey) or verification key (PublicKey) must be provided"
            }
        }
    }

    /**
     * Determines if this message signer is capable of signing messages.
     *
     * @param key the private key to use for signing. If `null`, the configured signing key will be used.
     * @return `true` if this message signer is configured for signing and has a private key; `false` if not.
     */
    fun canSignMessages(key: PrivateKey? = signingKey): Boolean = signingEnabled && key != null

    /**
     * Signs the provided `message`, overwriting an existing signature field inside the message object.
     *
     * @param message the message to be signed
     * @param key the private key to use for signing. If `null`, the configured signing key will be used.
     * @return Returns the signed (unwrapped) message. If signing is disabled through configuration, the message will be
     *   returned unchanged.
     * @throws IllegalStateException if this message signer has a public key for signature verification, but does not
     *   have the private key needed for signing messages.
     * @throws UncheckedIOException if determining the bytes for the message throws an IOException.
     * @throws UncheckedSecurityException if the signing process throws a SignatureException.
     */
    fun <T> signUsingField(message: FlexibleSignableMessageWrapper<T>, key: PrivateKey? = signingKey): T {
        if (signingEnabled) {
            val signatureBytes = signature(message, key)
            message.setSignature(signatureBytes)
        }
        return message.message
    }

    /**
     * Signs the provided `producerRecord` in the header, overwriting an existing signature, if a non-null value is
     * already set.
     *
     * @param producerRecord the record to be signed
     * @param key the private key to use for signing. If `null`, the configured signing key will be used.
     * @return Returns the record with a signature in the header. If signing is disabled through configuration, the
     *   record will be returned unchanged.
     * @throws IllegalStateException if this message signer has a public key for signature verification, but does not
     *   have the private key needed for signing messages.
     * @throws UncheckedIOException if determining the bytes for the message throws an IOException.
     * @throws UncheckedSecurityException if the signing process throws a SignatureException.
     */
    fun <ValueType : SpecificRecordBase> signUsingHeader(
        producerRecord: ProducerRecord<String, ValueType>,
        key: PrivateKey? = signingKey,
    ): ProducerRecord<String, ValueType> {
        if (signingEnabled) {
            val signature = signature(producerRecord, key)
            producerRecord.headers().add(RECORD_HEADER_KEY_SIGNATURE, signature.array())
        }
        return producerRecord
    }

    /**
     * Signs the provided `producerRecord` in the header, overwriting an existing signature, if a non-null value is
     * already set.
     *
     * @param producerRecord the record to be signed
     * @param key the private key to use for signing. If `null`, the configured signing key will be used.
     * @return Returns the record with a signature in the header. If signing is disabled through configuration, the
     *   record will be returned unchanged.
     * @throws IllegalStateException if this message signer has a public key for signature verification, but does not
     *   have the private key needed for signing messages.
     * @throws UncheckedIOException if determining the bytes for the message throws an IOException.
     * @throws UncheckedSecurityException if the signing process throws a SignatureException.
     */
    fun signByteArrayRecordUsingHeader(
        producerRecord: ProducerRecord<String, ByteArray>,
        key: PrivateKey? = signingKey,
    ): ProducerRecord<String, ByteArray> {
        if (signingEnabled) {
            val signature = signatureByteArray(producerRecord, key)
            producerRecord.headers().add(RECORD_HEADER_KEY_SIGNATURE, signature.array())
        }
        return producerRecord
    }

    /**
     * Determines the signature for the given `message`.
     *
     * The value for the signature in the message will be set to `null` to properly determine the signature, but is
     * restored to its original value before this method returns.
     *
     * @param message the message to be signed
     * @param key the private key to use for signing.
     * @return the signature for the message
     * @throws IllegalStateException if this message signer has a public key for signature verification, but does not
     *   have the private key needed for signing messages.
     * @throws UncheckedIOException if determining the bytes for the message throws an IOException.
     * @throws UncheckedSecurityException if the signing process throws a SignatureException.
     */
    private fun signature(message: FlexibleSignableMessageWrapper<*>, key: PrivateKey?): ByteBuffer {
        check(canSignMessages(key)) { KEY_NOT_FOR_SIGNING }
        val oldSignature = message.getSignature()
        message.clearSignature()
        val byteBuffer = toByteBuffer(message)
        try {
            return signature(byteBuffer, key!!)
        } catch (e: SignatureException) {
            throw UncheckedSecurityException(UNABLE_TO_SIGN_MESSAGE, e)
        } finally {
            oldSignature?.let { message.setSignature(it) }
        }
    }

    /**
     * Determines the signature for the given `producerRecord`.
     *
     * The value for the signature in the record will be set to `null` to properly determine the signature, but is
     * restored to its original value before this method returns.
     *
     * @param producerRecord the record to be signed
     * @param key the private key to use for signing.
     * @return the signature for the record
     * @throws IllegalStateException if this message signer has a public key for signature verification, but does not
     *   have the private key needed for signing messages.
     * @throws UncheckedIOException if determining the bytes throws an IOException.
     * @throws UncheckedSecurityException if the signing process throws a SignatureException.
     */
    private fun signature(
        producerRecord: ProducerRecord<String, out SpecificRecordBase>,
        key: PrivateKey?,
    ): ByteBuffer {
        check(canSignMessages(key)) { KEY_NOT_FOR_SIGNING }
        val specificRecordBase = producerRecord.value()
        val byteBuffer = toByteBuffer(specificRecordBase)
        try {
            return signature(byteBuffer, key!!)
        } catch (e: SignatureException) {
            throw UncheckedSecurityException(UNABLE_TO_SIGN_MESSAGE, e)
        }
    }

    private fun signatureByteArray(producerRecord: ProducerRecord<String, ByteArray>, key: PrivateKey?): ByteBuffer {
        check(canSignMessages(key)) { KEY_NOT_FOR_SIGNING }
        val byteBuffer = ByteBuffer.wrap(producerRecord.value())
        try {
            return signature(byteBuffer, key!!)
        } catch (e: SignatureException) {
            throw UncheckedSecurityException(UNABLE_TO_SIGN_MESSAGE, e)
        }
    }

    private fun signature(byteBuffer: ByteBuffer, key: PrivateKey): ByteBuffer {
        val messageBytes: ByteBuffer =
            if (stripAvroHeader) {
                stripAvroHeader(byteBuffer)
            } else {
                byteBuffer
            }
        val signingSignature = signatureInstance(signatureAlgorithm, signatureProvider, key)
        signingSignature.update(messageBytes)
        return ByteBuffer.wrap(signingSignature.sign())
    }

    /**
     * Determines if this message signer is capable of verifying message signatures.
     *
     * @param key the public key to use for verification. If `null`, the configured verification key will be used.
     * @return `true` if this message signer is configured for signature verification and has a public key; `false` if
     *   not.
     */
    fun canVerifyMessageSignatures(key: PublicKey? = verificationKey): Boolean {
        return signingEnabled && key != null
    }

    /**
     * Verifies the signature of the provided `message` using the signature in a message field.
     *
     * @param message the message to be verified
     * @param key the public key to use for verification. If `null`, the configured verification key will be used.
     * @return `true` if the signature of the given `message` was verified; `false` if not.
     */
    fun <T> verifyUsingField(message: FlexibleSignableMessageWrapper<T>, key: PublicKey? = verificationKey): Boolean {
        if (!canVerifyMessageSignatures(key)) {
            logger.error(KEY_NOT_FOR_VERIFICATION)
            return false
        }

        val messageSignature = message.getSignature()

        if (messageSignature == null) {
            logger.error("This message does not contain a signature")
            return false
        }

        try {
            message.clearSignature()
            return verifySignatureBytes(messageSignature, toByteBuffer(message), key!!)
        } catch (e: Exception) {
            logger.error(UNABLE_TO_VERIFY_SIGNATURE, e)
            return false
        } finally {
            message.setSignature(messageSignature)
        }
    }

    /**
     * Verifies the signature of the provided `message` using the previous verification key and the signature in a
     * message field.
     */
    fun <T> verifyUsingFieldWithPreviousKey(message: FlexibleSignableMessageWrapper<T>): Boolean =
        verifyUsingField(message, previousVerificationKey)

    /**
     * Verifies the signature of the provided `consumerRecord` using the signature from the message header.
     *
     * @param consumerRecord the record to be verified
     * @param key the public key to use for verification. If `null`, the configured verification key will be used.
     * @return `true` if the signature of the given `consumerRecord` was verified; `false` if not.
     */
    fun verifyUsingHeader(
        consumerRecord: ConsumerRecord<String, out SpecificRecordBase>,
        key: PublicKey? = verificationKey,
    ): Boolean {
        if (!canVerifyMessageSignatures(key)) {
            logger.error(KEY_NOT_FOR_VERIFICATION)
            return false
        }
        try {
            val signatureBytes = getSignatureBytes(consumerRecord)
            val specificRecordBase: SpecificRecordBase = consumerRecord.value()
            return verifySignatureBytes(ByteBuffer.wrap(signatureBytes), toByteBuffer(specificRecordBase), key!!)
        } catch (e: Exception) {
            logger.error(UNABLE_TO_VERIFY_SIGNATURE, e)
            return false
        }
    }

    /**
     * Verifies the signature of the provided `consumerRecord` using the previous verification key and the signature
     * from the message header.
     */
    fun verifyUsingHeaderWithPreviousKey(consumerRecord: ConsumerRecord<String, out SpecificRecordBase>): Boolean =
        verifyUsingHeader(consumerRecord, previousVerificationKey)

    /**
     * Verifies the signature of the provided `consumerRecord` using the signature from the message header.
     *
     * @param consumerRecord the record to be verified
     * @param key the public key to use for verification. If `null`, the configured verification key will be used.
     * @return `true` if the signature of the given `consumerRecord` was verified; `false` if not.
     */
    fun verifyByteArrayRecordUsingHeader(
        consumerRecord: ConsumerRecord<String, ByteArray>,
        key: PublicKey? = verificationKey,
    ): Boolean {
        try {
            if (!canVerifyMessageSignatures(key)) {
                logger.error(KEY_NOT_FOR_VERIFICATION)
                return false
            }
            val signatureBytes = getSignatureBytes(consumerRecord)
            return verifySignatureBytes(ByteBuffer.wrap(signatureBytes), ByteBuffer.wrap(consumerRecord.value()), key!!)
        } catch (e: Exception) {
            logger.error(UNABLE_TO_VERIFY_SIGNATURE, e)
            return false
        }
    }

    /**
     * Verifies the signature of the provided `consumerRecord` using the previous verification key and the signature
     * from the message header.
     */
    fun verifyByteArrayRecordUsingHeaderWithPreviousKey(consumerRecord: ConsumerRecord<String, ByteArray>): Boolean =
        verifyByteArrayRecordUsingHeader(consumerRecord, previousVerificationKey)

    private fun <ValueType : Any> getSignatureBytes(consumerRecord: ConsumerRecord<String, ValueType>): ByteArray {
        val header =
            consumerRecord.headers().lastHeader(RECORD_HEADER_KEY_SIGNATURE)
                ?: throw IllegalArgumentException("This ConsumerRecord does not contain a signature header")

        val signatureBytes = header.value()
        require(!(signatureBytes == null || signatureBytes.isEmpty())) { "Signature header is empty" }
        return signatureBytes
    }

    @Throws(SignatureException::class)
    private fun verifySignatureBytes(
        signatureBytes: ByteBuffer,
        messageByteBuffer: ByteBuffer,
        key: PublicKey,
    ): Boolean {
        val messageBytes: ByteBuffer =
            if (stripAvroHeader) {
                stripAvroHeader(messageByteBuffer)
            } else {
                messageByteBuffer
            }
        val verificationSignature = signatureInstance(signatureAlgorithm, signatureProvider, key)
        verificationSignature.update(messageBytes)
        return verificationSignature.verify(signatureBytes.array())
    }

    private fun hasAvroHeader(bytes: ByteBuffer): Boolean {
        return (bytes.array().size >= AVRO_HEADER_LENGTH) &&
            ((bytes[0].toInt() and 0xFF) == 0xC3) &&
            ((bytes[1].toInt() and 0xFF) == 0x01)
    }

    private fun stripAvroHeader(bytes: ByteBuffer): ByteBuffer {
        if (hasAvroHeader(bytes)) {
            return ByteBuffer.wrap(bytes.array().copyOfRange(AVRO_HEADER_LENGTH, bytes.array().size))
        }
        return bytes
    }

    private fun toByteBuffer(message: FlexibleSignableMessageWrapper<*>): ByteBuffer {
        try {
            return message.toByteBuffer()
        } catch (e: IOException) {
            throw UncheckedIOException("Unable to determine bytes for message", e)
        }
    }

    private fun toByteBuffer(message: SpecificRecordBase): ByteBuffer {
        try {
            return ByteBuffer.wrap(AvroEncoder.encode(message))
        } catch (e: IOException) {
            throw UncheckedIOException("Unable to determine bytes for message", e)
        }
    }

    override fun toString(): String {
        return String.format(
            "MessageSigner[algorithm=\"%s\"-\"%s\", provider=\"%s\", sign=%b, verify=%b]",
            signatureAlgorithm,
            keyAlgorithm,
            signatureProvider,
            canSignMessages(),
            canVerifyMessageSignatures(),
        )
    }

    companion object {
        // Two magic bytes (0xC3, 0x01) followed by an 8-byte fingerprint
        const val AVRO_HEADER_LENGTH: Int = 10

        const val DEFAULT_SIGNATURE_ALGORITHM: String = "SHA256withRSA"
        const val DEFAULT_SIGNATURE_PROVIDER: String = "SunRsaSign"
        const val DEFAULT_KEY_ALGORITHM: String = "RSA"

        const val RECORD_HEADER_KEY_SIGNATURE: String = "signature"

        private val PEM_REMOVAL_PATTERN: Pattern = Pattern.compile("-----(?:BEGIN|END) .*?-----|\\r|\\n")

        const val KEY_NOT_FOR_SIGNING = "This MessageSigner is not configured for signing"
        const val KEY_NOT_FOR_VERIFICATION = "This MessageSigner is not configured for verification"
        const val UNABLE_TO_SIGN_MESSAGE = "Unable to sign message"
        const val UNABLE_TO_VERIFY_SIGNATURE = "Unable to verify message signature"

        val logger = KotlinLogging.logger {}

        @JvmStatic
        private fun signatureInstance(
            signatureAlgorithm: String,
            signatureProvider: String?,
            signingKey: PrivateKey,
        ): Signature {
            val signature = signatureInstance(signatureAlgorithm, signatureProvider)
            try {
                signature.initSign(signingKey)
            } catch (e: InvalidKeyException) {
                throw UncheckedSecurityException(cause = e)
            }
            return signature
        }

        @JvmStatic
        private fun signatureInstance(
            signatureAlgorithm: String,
            signatureProvider: String?,
            verificationKey: PublicKey,
        ): Signature {
            val signature = signatureInstance(signatureAlgorithm, signatureProvider)
            try {
                signature.initVerify(verificationKey)
            } catch (e: InvalidKeyException) {
                throw UncheckedSecurityException(cause = e)
            }
            return signature
        }

        @JvmStatic
        private fun signatureInstance(signatureAlgorithm: String, signatureProvider: String?): Signature {
            try {
                if (signatureProvider == null) {
                    return Signature.getInstance(signatureAlgorithm)
                }
                return Signature.getInstance(signatureAlgorithm, signatureProvider)
            } catch (e: GeneralSecurityException) {
                throw UncheckedSecurityException("Unable to create Signature for Avro Messages", e)
            }
        }

        fun readPrivateKey(privateKeyFile: Resource?): PrivateKey? {
            if (privateKeyFile == null) {
                logger.debug { "No private key file provided" }
                return null
            }
            try {
                val content = privateKeyFile.getContentAsString(StandardCharsets.ISO_8859_1)
                return PemContent.of(content).privateKey
            } catch (e: IOException) {
                logger.error(e) { "Unable to read ${privateKeyFile.filename} as ISO-LATIN-1 PEM text" }
                throw UncheckedIOException(e)
            }
        }

        private fun readPublicKey(keyAlgorithm: String, publicKeyFile: Resource?): PublicKey? {
            if (publicKeyFile == null) {
                logger.debug { "No public key file provided" }
                return null
            }
            val content = publicKeyFile.getContentAsString(StandardCharsets.ISO_8859_1)
            val base64 = PEM_REMOVAL_PATTERN.matcher(content).replaceAll("")
            val bytes = Base64.getDecoder().decode(base64)
            val keySpec = X509EncodedKeySpec(bytes)
            try {
                return KeyFactory.getInstance(keyAlgorithm).generatePublic(keySpec)
            } catch (e: GeneralSecurityException) {
                logger.error(e) { "Unable to read ${publicKeyFile.filename} as ISO-LATIN-1 PEM text" }
                throw UncheckedSecurityException(cause = e)
            }
        }
    }

    // For backwards compatibility
    @Deprecated("Call with FlexibleSignableMessageWrapper instead. This method will be removed in a future release.")
    fun <T> signUsingField(message: SignableMessageWrapper<T>): T = signUsingField(message.toFlexibleWrapper())

    @Deprecated("Call with FlexibleSignableMessageWrapper instead. This method will be removed in a future release.")
    fun <T> verifyUsingField(message: SignableMessageWrapper<T>): Boolean =
        verifyUsingField(message.toFlexibleWrapper())
}

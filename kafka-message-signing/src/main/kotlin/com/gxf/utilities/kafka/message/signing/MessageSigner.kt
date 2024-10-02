// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing

import com.gxf.utilities.kafka.avro.AvroEncoder
import com.gxf.utilities.kafka.message.wrapper.SignableMessageWrapper
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.regex.Pattern
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

    init {
        if (properties.signingEnabled) {
            require(!(signingKey == null && verificationKey == null)) {
                "A signing key (PrivateKey) or verification key (PublicKey) must be provided"
            }
        }
    }

    fun canSignMessages(): Boolean {
        return this.signingEnabled && this.signingKey != null
    }

    /**
     * Signs the provided `message`, overwriting an existing signature field inside the message object.
     *
     * @param message the message to be signed
     * @return Returns the signed (unwrapped) message. If signing is disabled through configuration, the message will be
     *   returned unchanged.
     * @throws IllegalStateException if this message signer has a public key for signature verification, but does not
     *   have the private key needed for signing messages.
     * @throws UncheckedIOException if determining the bytes for the message throws an IOException.
     * @throws UncheckedSecurityException if the signing process throws a SignatureException.
     */
    fun <T> signUsingField(message: SignableMessageWrapper<T>): T {
        if (this.signingEnabled) {
            val signatureBytes = this.signature(message)
            message.setSignature(signatureBytes)
        }
        return message.message
    }

    /**
     * Signs the provided `producerRecord` in the header, overwriting an existing signature, if a non-null value is
     * already set.
     *
     * @param producerRecord the record to be signed
     * @throws IllegalStateException if this message signer has a public key for signature verification, but does not
     *   have the private key needed for signing messages.
     * @throws UncheckedIOException if determining the bytes for the message throws an IOException.
     * @throws UncheckedSecurityException if the signing process throws a SignatureException.
     */
    fun signUsingHeader(
        producerRecord: ProducerRecord<String, out SpecificRecordBase>
    ): ProducerRecord<String, out SpecificRecordBase> {
        if (this.signingEnabled) {
            val signature = this.signature(producerRecord)
            producerRecord.headers().add(RECORD_HEADER_KEY_SIGNATURE, signature)
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
     * @return the signature for the message
     * @throws IllegalStateException if this message signer has a public key for signature verification, but does not
     *   have the private key needed for signing messages.
     * @throws UncheckedIOException if determining the bytes for the message throws an IOException.
     * @throws UncheckedSecurityException if the signing process throws a SignatureException.
     */
    private fun signature(message: SignableMessageWrapper<*>): ByteArray {
        check(this.canSignMessages()) {
            "This MessageSigner is not configured for signing, it can only be used for verification"
        }
        val oldSignature = message.getSignature()
        message.setSignature(null)
        val byteArray = this.toByteArray(message)
        try {
            return signature(byteArray)
        } catch (e: SignatureException) {
            throw UncheckedSecurityException("Unable to sign message", e)
        } finally {
            message.setSignature(oldSignature)
        }
    }

    /**
     * Determines the signature for the given `producerRecord`.
     *
     * The value for the signature in the record will be set to `null` to properly determine the signature, but is
     * restored to its original value before this method returns.
     *
     * @param producerRecord the record to be signed
     * @return the signature for the record
     * @throws IllegalStateException if this message signer has a public key for signature verification, but does not
     *   have the private key needed for signing messages.
     * @throws UncheckedIOException if determining the bytes throws an IOException.
     * @throws UncheckedSecurityException if the signing process throws a SignatureException.
     */
    private fun signature(producerRecord: ProducerRecord<String, out SpecificRecordBase>): ByteArray {
        check(this.canSignMessages()) {
            "This MessageSigner is not configured for signing, it can only be used for verification"
        }
        val oldSignatureHeader = producerRecord.headers().lastHeader(RECORD_HEADER_KEY_SIGNATURE)
        producerRecord.headers().remove(RECORD_HEADER_KEY_SIGNATURE)
        val specificRecordBase = producerRecord.value()
        val byteArray = this.toByteArray(specificRecordBase)
        try {
            return signature(byteArray)
        } catch (e: SignatureException) {
            throw UncheckedSecurityException("Unable to sign message", e)
        } finally {
            if (oldSignatureHeader != null) {
                producerRecord.headers().add(RECORD_HEADER_KEY_SIGNATURE, oldSignatureHeader.value())
            }
        }
    }

    private fun signature(byteArray: ByteArray): ByteArray {
        val messageBytes: ByteArray =
            if (this.stripAvroHeader) {
                this.stripAvroHeader(byteArray)
            } else {
                byteArray
            }
        val signingSignature = signatureInstance(signatureAlgorithm, signatureProvider, signingKey!!)
        signingSignature.update(messageBytes)
        return signingSignature.sign()
    }

    fun canVerifyMessageSignatures(): Boolean {
        return this.signingEnabled && this.verificationKey != null
    }

    /**
     * Verifies the signature of the provided `message` using the signature in a message field.
     *
     * @param message the message to be verified
     * @return `true` if the signature of the given `message` was verified; `false` if not.
     */
    fun <T> verifyUsingField(message: SignableMessageWrapper<T>): Boolean {
        if (!this.canVerifyMessageSignatures()) {
            logger.error("This MessageSigner is not configured for verification, it can only be used for signing")
            return false
        }

        val messageSignature = message.getSignature()

        if (messageSignature == null || messageSignature.isEmpty()) {
            logger.error("This message does not contain a signature")
            return false
        }

        try {
            message.setSignature(null)
            return this.verifySignatureBytes(messageSignature, this.toByteArray(message))
        } catch (e: Exception) {
            logger.error("Unable to verify message signature", e)
            return false
        } finally {
            message.setSignature(messageSignature)
        }
    }

    /**
     * Verifies the signature of the provided `consumerRecord` using the signature from the message header.
     *
     * @param consumerRecord the record to be verified
     * @return `true` if the signature of the given `consumerRecord` was verified; `false` if not. SignatureException.
     */
    fun verifyUsingHeader(consumerRecord: ConsumerRecord<String, out SpecificRecordBase>): Boolean {
        if (!this.canVerifyMessageSignatures()) {
            logger.error("This MessageSigner is not configured for verification, it can only be used for signing")
            return false
        }

        val header = consumerRecord.headers().lastHeader(RECORD_HEADER_KEY_SIGNATURE)
        if (header == null) {
            logger.error("This ProducerRecord does not contain a signature header")
            return false
        }

        val signatureBytes = header.value()
        if (signatureBytes == null || signatureBytes.isEmpty()) {
            logger.error("Signature header is empty")
            return false
        }

        try {
            val specificRecordBase: SpecificRecordBase = consumerRecord.value()
            return this.verifySignatureBytes(signatureBytes, this.toByteArray(specificRecordBase))
        } catch (e: Exception) {
            logger.error("Unable to verify message signature", e)
            return false
        }
    }

    @Throws(SignatureException::class)
    private fun verifySignatureBytes(signatureBytes: ByteArray, messageByteArray: ByteArray): Boolean {
        val messageBytes: ByteArray =
            if (this.stripAvroHeader) {
                this.stripAvroHeader(messageByteArray)
            } else {
                messageByteArray
            }
        val verificationSignature = signatureInstance(signatureAlgorithm, signatureProvider, verificationKey!!)
        verificationSignature.update(messageBytes)
        return verificationSignature.verify(signatureBytes)
    }

    private fun hasAvroHeader(bytes: ByteArray): Boolean {
        return (bytes.size >= AVRO_HEADER_LENGTH) &&
            ((bytes[0].toInt() and 0xFF) == 0xC3) &&
            ((bytes[1].toInt() and 0xFF) == 0x01)
    }

    private fun stripAvroHeader(bytes: ByteArray): ByteArray {
        if (this.hasAvroHeader(bytes)) {
            return Arrays.copyOfRange(bytes, AVRO_HEADER_LENGTH, bytes.size)
        }
        return bytes
    }

    private fun toByteArray(message: SignableMessageWrapper<*>): ByteArray {
        try {
            return message.toByteArray()
        } catch (e: IOException) {
            throw UncheckedIOException("Unable to determine bytes for message", e)
        }
    }

    private fun toByteArray(message: SpecificRecordBase): ByteArray {
        try {
            return AvroEncoder.encode(message)
        } catch (e: IOException) {
            throw UncheckedIOException("Unable to determine bytes for message", e)
        }
    }

    override fun toString(): String {
        return String.format(
            "MessageSigner[algorithm=\"%s\"-\"%s\", provider=\"%s\", sign=%b, verify=%b]",
            this.signatureAlgorithm,
            this.keyAlgorithm,
            this.signatureProvider,
            this.canSignMessages(),
            this.canVerifyMessageSignatures())
    }

    companion object {
        // Two magic bytes (0xC3, 0x01) followed by an 8-byte fingerprint
        const val AVRO_HEADER_LENGTH: Int = 10

        const val DEFAULT_SIGNATURE_ALGORITHM: String = "SHA256withRSA"
        const val DEFAULT_SIGNATURE_PROVIDER: String = "SunRsaSign"
        const val DEFAULT_KEY_ALGORITHM: String = "RSA"

        const val RECORD_HEADER_KEY_SIGNATURE: String = "signature"

        private val PEM_REMOVAL_PATTERN: Pattern = Pattern.compile("-----(?:BEGIN|END) .*?-----|\\r|\\n")

        val logger: Logger = LoggerFactory.getLogger(this::class.java)

        @JvmStatic
        private fun signatureInstance(
            signatureAlgorithm: String,
            signatureProvider: String?,
            signingKey: PrivateKey
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
            verificationKey: PublicKey
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
                return null
            }
            try {
                val content = privateKeyFile.getContentAsString(StandardCharsets.ISO_8859_1)
                return PemContent.of(content).privateKey
            } catch (e: IOException) {
                throw UncheckedIOException("Unable to read ${privateKeyFile.filename} as ISO-LATIN-1 PEM text", e)
            }
        }

        private fun readPublicKey(keyAlgorithm: String, publicKeyFile: Resource?): PublicKey? {
            if (publicKeyFile == null) {
                return null
            }
            val content = publicKeyFile.getContentAsString(StandardCharsets.ISO_8859_1)
            val base64 = PEM_REMOVAL_PATTERN.matcher(content).replaceAll("")
            val bytes = Base64.getDecoder().decode(base64)
            val keySpec = X509EncodedKeySpec(bytes)
            return try {
                KeyFactory.getInstance(keyAlgorithm).generatePublic(keySpec)
            } catch (e: GeneralSecurityException) {
                throw UncheckedSecurityException(cause = e)
            }
        }
    }
}

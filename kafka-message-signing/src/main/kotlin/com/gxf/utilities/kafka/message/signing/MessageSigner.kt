// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package com.gxf.utilities.kafka.message.signing

import com.gxf.utilities.kafka.message.wrapper.SignableMessageWrapper
import org.apache.avro.message.BinaryMessageEncoder
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.ssl.pem.PemContent
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.regex.Pattern

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
            require(!(signingKey == null && verificationKey == null)) { "A signing key (PrivateKey) or verification key (PublicKey) must be provided" }
        }
    }

    fun canSignMessages(): Boolean {
        return this.signingEnabled && this.signingKey != null
    }

    /**
     * Signs the provided `message`, overwriting an existing signature field inside the message object.
     *
     * @param message the message to be signed
     * @return Returns the signed (unwrapped) message. If signing is disabled through configuration, the message will be returned unchanged.
     *
     * @throws IllegalStateException      if this message signer has a public key for signature
     * verification, but does not have the private key needed for signing messages.
     * @throws UncheckedIOException       if determining the bytes for the message throws an IOException.
     * @throws UncheckedSecurityException if the signing process throws a SignatureException.
     */
    fun <T> signUsingField(message: SignableMessageWrapper<T>): T {
        if (this.signingEnabled) {
            val signatureBytes = this.signature(message)
            message.setSignature(ByteBuffer.wrap(signatureBytes))
        }
        return message.message
    }

    /**
     * Signs the provided `producerRecord` in the header, overwriting an existing signature, if a non-null value is
     * already set.
     *
     * @param producerRecord the record to be signed
     * @throws IllegalStateException      if this message signer has a public key for signature
     * verification, but does not have the private key needed for signing messages.
     * @throws UncheckedIOException       if determining the bytes for the message throws an IOException.
     * @throws UncheckedSecurityException if the signing process throws a SignatureException.
     */
    fun signUsingHeader(producerRecord: ProducerRecord<String, out SpecificRecordBase>): ProducerRecord<String, out SpecificRecordBase> {
        if (this.signingEnabled) {
            val signature = this.signature(producerRecord)
            producerRecord.headers().add(RECORD_HEADER_KEY_SIGNATURE, signature)
        }
        return producerRecord
    }

    /**
     * Determines the signature for the given `message`.
     *
     *
     * The value for the signature in the message will be set to `null` to properly determine
     * the signature, but is restored to its original value before this method returns.
     *
     * @param message the message to be signed
     * @return the signature for the message
     * @throws IllegalStateException      if this message signer has a public key for signature
     * verification, but does not have the private key needed for signing messages.
     * @throws UncheckedIOException       if determining the bytes for the message throws an IOException.
     * @throws UncheckedSecurityException if the signing process throws a SignatureException.
     */
    private fun signature(message: SignableMessageWrapper<*>): ByteArray {
        check(this.canSignMessages()) { "This MessageSigner is not configured for signing, it can only be used for verification" }
        val oldSignature = message.getSignature()
        try {
            message.setSignature(null)
            val messageBytes: ByteArray = if (this.stripAvroHeader) {
                this.stripAvroHeader(this.toByteBuffer(message))
            } else {
                this.toByteBuffer(message)!!.array()
            }
            val signingSignature = signatureInstance(signatureAlgorithm, signatureProvider, signingKey!!)
            signingSignature.update(messageBytes)
            return signingSignature.sign()
        } catch (e: SignatureException) {
            throw UncheckedSecurityException("Unable to sign message", e)
        } finally {
            message.setSignature(oldSignature)
        }
    }

    /**
     * Determines the signature for the given `producerRecord`.
     *
     *
     * The value for the signature in the record will be set to `null` to properly determine
     * the signature, but is restored to its original value before this method returns.
     *
     * @param producerRecord the record to be signed
     * @return the signature for the record
     * @throws IllegalStateException      if this message signer has a public key for signature
     * verification, but does not have the private key needed for signing messages.
     * @throws UncheckedIOException       if determining the bytes throws an IOException.
     * @throws UncheckedSecurityException if the signing process throws a SignatureException.
     */
    private fun signature(producerRecord: ProducerRecord<String, out SpecificRecordBase>): ByteArray {
        check(this.canSignMessages()) { "This MessageSigner is not configured for signing, it can only be used for verification" }
        val oldSignatureHeader = producerRecord.headers().lastHeader(RECORD_HEADER_KEY_SIGNATURE)
        try {
            producerRecord.headers().remove(RECORD_HEADER_KEY_SIGNATURE)
            val specificRecordBase = producerRecord.value()
            val messageBytes: ByteArray = if (this.stripAvroHeader) {
                this.stripAvroHeader(this.toByteBuffer(specificRecordBase))
            } else {
                this.toByteBuffer(specificRecordBase).array()
            }
            val signingSignature = signatureInstance(signatureAlgorithm, signatureProvider, signingKey!!)
            signingSignature.update(messageBytes)
            return signingSignature.sign()
        } catch (e: SignatureException) {
            throw UncheckedSecurityException("Unable to sign message", e)
        } finally {
            if (oldSignatureHeader != null) {
                producerRecord.headers().add(RECORD_HEADER_KEY_SIGNATURE, oldSignatureHeader.value())
            }
        }
    }

    fun canVerifyMessageSignatures(): Boolean {
        return this.signingEnabled && this.verificationKey != null
    }

    /**
     * Verifies the signature of the provided `message` using the signature in a message field.
     *
     * @param message the message to be verified
     * @return `true` if the signature of the given `message` was verified; `false`
     * if not.
     * @throws IllegalStateException      if this message signer has a private key needed for signing
     * messages, but does not have the public key for signature verification.
     * @throws UncheckedIOException       if determining the bytes for the message throws an IOException.
     * @throws UncheckedSecurityException if the signature verification process throws a
     * SignatureException.
     */
    fun verifyUsingField(message: SignableMessageWrapper<*>): Boolean {
        check(this.canVerifyMessageSignatures()) { "This MessageSigner is not configured for verification, it can only be used for signing" }

        val messageSignature = message.getSignature() ?: return false
        messageSignature.mark()
        val signatureBytes = ByteArray(messageSignature.remaining())
        messageSignature[signatureBytes]

        try {
            message.setSignature(null)
            return this.verifySignatureBytes(signatureBytes, this.toByteBuffer(message))
        } catch (e: SignatureException) {
            throw UncheckedSecurityException("Unable to verify message signature", e)
        } finally {
            messageSignature.reset()
            message.setSignature(messageSignature)
        }
    }

    fun <T> verifyUsingFieldThrowingException(message: SignableMessageWrapper<T>): T {
        if(this.verifyUsingField(message)) {
            return message.message
        } else {
            throw VerificationException("Verification of message signing failed")
        }
    }

    /**
     * Verifies the signature of the provided `consumerRecord` using the signature from the message header.
     *
     * @param consumerRecord the record to be verified
     * @return `true` if the signature of the given `consumerRecord` was verified; `false`
     * if not.
     * @throws IllegalStateException      if this message signer has a private key needed for signing
     * messages, but does not have the public key for signature verification.
     * @throws UncheckedIOException       if determining the bytes throws an IOException.
     * @throws UncheckedSecurityException if the signature verification process throws a
     * SignatureException.
     */
    fun verifyUsingHeader(consumerRecord: ConsumerRecord<String, out SpecificRecordBase>): Boolean {
        check(this.canVerifyMessageSignatures()) { "This MessageSigner is not configured for verification, it can only be used for signing" }

        val header = consumerRecord.headers().lastHeader(RECORD_HEADER_KEY_SIGNATURE)
            ?: throw IllegalStateException(
                "This ProducerRecord does not contain a signature header"
            )
        val signatureBytes = header.value()
        check(!(signatureBytes == null || signatureBytes.isEmpty())) { "Signature header is empty" }

        try {
            consumerRecord.headers().remove(RECORD_HEADER_KEY_SIGNATURE)
            val specificRecordBase: SpecificRecordBase = consumerRecord.value()
            return this.verifySignatureBytes(
                signatureBytes,
                this.toByteBuffer(specificRecordBase)
            )
        } catch (e: SignatureException) {
            throw UncheckedSecurityException("Unable to verify message signature", e)
        }
    }

    fun verifyUsingHeaderThrowingException(consumerRecord: ConsumerRecord<String, out SpecificRecordBase>): ConsumerRecord<String, out SpecificRecordBase> {
        if(this.verifyUsingHeader(consumerRecord)) {
            return consumerRecord
        } else {
            throw VerificationException("Verification of record signing failed")
        }
    }

    @Throws(SignatureException::class)
    private fun verifySignatureBytes(signatureBytes: ByteArray, messageByteBuffer: ByteBuffer?): Boolean {
        val messageBytes: ByteArray = if (this.stripAvroHeader) {
            this.stripAvroHeader(messageByteBuffer)
        } else {
            messageByteBuffer!!.array()
        }
        val verificationSignature = signatureInstance(signatureAlgorithm, signatureProvider, verificationKey!!)
        verificationSignature.update(messageBytes)
        return verificationSignature.verify(signatureBytes)
    }

    private fun hasAvroHeader(bytes: ByteArray): Boolean {
        return (bytes.size >= AVRO_HEADER_LENGTH)
                && ((bytes[0].toInt() and 0xFF) == 0xC3)
                && ((bytes[1].toInt() and 0xFF) == 0x01)
    }

    private fun stripAvroHeader(byteBuffer: ByteBuffer?): ByteArray {
        val bytes = ByteArray(byteBuffer!!.remaining())
        byteBuffer[bytes]
        if (this.hasAvroHeader(bytes)) {
            return Arrays.copyOfRange(bytes, AVRO_HEADER_LENGTH, bytes.size)
        }
        return bytes
    }

    private fun toByteBuffer(message: SignableMessageWrapper<*>): ByteBuffer? {
        try {
            return message.toByteBuffer()
        } catch (e: IOException) {
            throw UncheckedIOException("Unable to determine ByteBuffer for Message", e)
        }
    }

    private fun toByteBuffer(message: SpecificRecordBase): ByteBuffer {
        try {
            return BinaryMessageEncoder<Any>(message.specificData, message.schema).encode(message)
        } catch (e: IOException) {
            throw UncheckedIOException("Unable to determine ByteBuffer for Message", e)
        }
    }

    override fun toString(): String {
        return String.format(
            "MessageSigner[algorithm=\"%s\"-\"%s\", provider=\"%s\", sign=%b, verify=%b]",
            this.signatureAlgorithm,
            this.keyAlgorithm,
            this.signatureProvider,
            this.canSignMessages(),
            this.canVerifyMessageSignatures()
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

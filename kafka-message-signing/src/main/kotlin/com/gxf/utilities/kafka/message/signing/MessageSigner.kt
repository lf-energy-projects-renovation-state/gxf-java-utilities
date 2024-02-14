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
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.ByteBuffer
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.regex.Pattern

@Component
// Only instantiate when no other bean has been configured
@ConditionalOnMissingBean(MessageSigner::class)
class MessageSigner(properties: MessageSigningProperties) {

    private var signingEnabled = false

    private var stripAvroHeader = false

    private var signatureAlgorithm: String? = null
    private var signatureProvider: String? = null
    private var signatureKeyAlgorithm: String? = null
    private var signatureKeySize = 0

    private var signingSignature: Signature? = null
    private var verificationSignature: Signature? = null

    private var signingKey: PrivateKey? = null
    private var verificationKey: PublicKey? = null

    init {
        this.signingEnabled = properties.signingEnabled
        if (this.signingEnabled) {
            this.signingSignature =
                signatureInstance(
                    properties.algorithm, properties.provider, properties.privateKey
                )
            this.verificationSignature =
                signatureInstance(
                    properties.algorithm, properties.provider, properties.publicKey
                )
            this.signingEnabled = properties.signingEnabled
            this.stripAvroHeader = properties.stripHeaders
            this.signatureAlgorithm = properties.algorithm
            this.signatureKeyAlgorithm = properties.keyAlgorithm
            this.signatureKeySize = properties.keySize
            require(!(properties.privateKey == null && properties.publicKey == null)) { "A signing key (PrivateKey) or verification key (PublicKey) must be provided" }
            this.signingKey = properties.privateKey
            this.verificationKey = properties.publicKey
            when {
                properties.provider != null -> this.signatureProvider = properties.provider
                this.signingSignature != null -> this.signatureProvider = signingSignature?.getProvider()?.name
                this.verificationSignature != null -> this.signatureProvider =
                    verificationSignature?.getProvider()?.name

                else -> {
                    // Should not happen, set to null and ignore.
                    this.signatureProvider = null
                }
            }
        }
    }

    fun canSignMessages(): Boolean {
        return this.signingEnabled && this.signingSignature != null
    }

    /**
     * Signs the provided `message`, overwriting an existing signature.
     *
     * @param message the message to be signed
     * @return Returns the signed (unwrapped) message. If signing is disabled through configuration, the message will be returned unchanged.
     *
     * @throws IllegalStateException      if this message signer has a public key for signature
     * verification, but does not have the private key needed for signing messages.
     * @throws UncheckedIOException       if determining the bytes for the message throws an IOException.
     * @throws UncheckedSecurityException if the signing process throws a SignatureException.
     */
    fun <T> sign(message: SignableMessageWrapper<T>): T {
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
    fun sign(producerRecord: ProducerRecord<String, out SpecificRecordBase>): ProducerRecord<String, out SpecificRecordBase> {
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
    fun signature(message: SignableMessageWrapper<*>): ByteArray {
        check(this.canSignMessages()) { "This MessageSigner is not configured for signing, it can only be used for verification" }
        val oldSignature = message.getSignature()
        try {
            message.setSignature(null)
            synchronized(signingSignature!!) {
                val messageBytes: ByteArray = if (this.stripAvroHeader) {
                    this.stripAvroHeader(this.toByteBuffer(message))
                } else {
                    this.toByteBuffer(message)!!.array()
                }
                signingSignature!!.update(messageBytes)
                return signingSignature!!.sign()
            }
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
    fun signature(producerRecord: ProducerRecord<String, out SpecificRecordBase>): ByteArray {
        check(this.canSignMessages()) { "This MessageSigner is not configured for signing, it can only be used for verification" }
        val oldSignatureHeader = producerRecord.headers().lastHeader(RECORD_HEADER_KEY_SIGNATURE)
        try {
            producerRecord.headers().remove(RECORD_HEADER_KEY_SIGNATURE)
            synchronized(signingSignature!!) {
                val specificRecordBase = producerRecord.value()
                val messageBytes: ByteArray = if (this.stripAvroHeader) {
                    this.stripAvroHeader(this.toByteBuffer(specificRecordBase))
                } else {
                    this.toByteBuffer(specificRecordBase).array()
                }
                signingSignature!!.update(messageBytes)
                return signingSignature!!.sign()
            }
        } catch (e: SignatureException) {
            throw UncheckedSecurityException("Unable to sign message", e)
        } finally {
            if (oldSignatureHeader != null) {
                producerRecord.headers().add(RECORD_HEADER_KEY_SIGNATURE, oldSignatureHeader.value())
            }
        }
    }

    fun canVerifyMessageSignatures(): Boolean {
        return this.signingEnabled && this.verificationSignature != null
    }

    /**
     * Verifies the signature of the provided `message`.
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
    fun verify(message: SignableMessageWrapper<*>): Boolean {
        check(this.canVerifyMessageSignatures()) { "This MessageSigner is not configured for verification, it can only be used for signing" }

        val messageSignature = message.getSignature() ?: return false
        messageSignature.mark()
        val signatureBytes = ByteArray(messageSignature.remaining())
        messageSignature.get(signatureBytes)

        try {
            message.setSignature(null)
            synchronized((verificationSignature)!!) {
                return this.verifySignatureBytes(signatureBytes, this.toByteBuffer(message))
            }
        } catch (e: SignatureException) {
            throw UncheckedSecurityException("Unable to verify message signature", e)
        } finally {
            messageSignature.reset()
            message.setSignature(messageSignature)
        }
    }

    /**
     * Verifies the signature of the provided `consumerRecord`.
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
    fun verify(consumerRecord: ConsumerRecord<String, out SpecificRecordBase>): Boolean {
        check(this.canVerifyMessageSignatures()) { "This MessageSigner is not configured for verification, it can only be used for signing" }

        val header = consumerRecord.headers().lastHeader(RECORD_HEADER_KEY_SIGNATURE)
            ?: throw IllegalStateException(
                "This ProducerRecord does not contain a signature header"
            )
        val signatureBytes = header.value()
        if (signatureBytes == null || signatureBytes.isEmpty()) {
            return false
        }

        try {
            consumerRecord.headers().remove(RECORD_HEADER_KEY_SIGNATURE)
            synchronized((verificationSignature)!!) {
                val specificRecordBase: SpecificRecordBase = consumerRecord.value()
                return this.verifySignatureBytes(
                    signatureBytes,
                    this.toByteBuffer(specificRecordBase)
                )
            }
        } catch (e: SignatureException) {
            throw UncheckedSecurityException("Unable to verify message signature", e)
        }
    }

    @Throws(SignatureException::class)
    private fun verifySignatureBytes(signatureBytes: ByteArray, messageByteBuffer: ByteBuffer?): Boolean {
        val messageBytes: ByteArray = if (this.stripAvroHeader) {
            this.stripAvroHeader(messageByteBuffer)
        } else {
            messageByteBuffer!!.array()
        }
        verificationSignature!!.update(messageBytes)
        return verificationSignature!!.verify(signatureBytes)
    }

    private fun hasAvroHeader(bytes: ByteArray): Boolean {
        return (bytes.size >= AVRO_HEADER_LENGTH)
                && ((bytes[0].toInt() and 0xFF) == 0xC3)
                && ((bytes[1].toInt() and 0xFF) == 0x01)
    }

    private fun stripAvroHeader(byteBuffer: ByteBuffer?): ByteArray {
        val bytes = ByteArray(byteBuffer!!.remaining())
        byteBuffer.get(bytes)
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

    fun isSigningEnabled(): Boolean {
        return this.signingEnabled
    }

    fun signingKey(): Optional<PrivateKey> {
        return Optional.ofNullable(this.signingKey)
    }

    fun signingKeyPem(): Optional<String> {
        return signingKey().map { key: PrivateKey ->
            this.keyAsMem(key, key.algorithm + " PRIVATE KEY")
        }
    }

    fun verificationKey(): Optional<PublicKey> {
        return Optional.ofNullable(this.verificationKey)
    }

    fun verificationKeyPem(): Optional<String> {
        return verificationKey()
            .map { key: PublicKey ->
                this.keyAsMem(key, key.algorithm + " PUBLIC KEY")
            }
    }

    private fun keyAsMem(key: Key, label: String): String {
        return ("-----BEGIN $label-----" + "\r\n"
                + Base64.getMimeEncoder().encodeToString(key.encoded) + "\r\n"
                + "-----END $label-----" + "\r\n")
    }

    override fun toString(): String {
        return String.format(
            "MessageSigner[algorithm=\"%s\"-\"%s\", provider=\"%s\", keySize=%d, sign=%b, verify=%b]",
            this.signatureAlgorithm,
            this.signatureKeyAlgorithm,
            this.signatureProvider,
            this.signatureKeySize,
            this.canSignMessages(),
            this.canVerifyMessageSignatures()
        )
    }

    fun descriptionWithKeys(): String {
        val sb = StringBuilder(this.toString())
        signingKeyPem().ifPresent { key: String ->
            sb.append(System.lineSeparator()).append(key)
        }
        verificationKeyPem().ifPresent { key: String ->
            sb.append(System.lineSeparator()).append(key)
        }
        return sb.toString()
    }

    companion object {
        // Two magic bytes (0xC3, 0x01) followed by an 8-byte fingerprint
        const val AVRO_HEADER_LENGTH: Int = 10

        const val DEFAULT_SIGNATURE_ALGORITHM: String = "SHA256withRSA"
        const val DEFAULT_SIGNATURE_PROVIDER: String = "SunRsaSign"
        const val DEFAULT_SIGNATURE_KEY_ALGORITHM: String = "RSA"
        const val DEFAULT_SIGNATURE_KEY_SIZE: Int = 2048

        const val RECORD_HEADER_KEY_SIGNATURE: String = "signature"

        @JvmStatic
        private fun signatureInstance(
            signatureAlgorithm: String,
            signatureProvider: String?,
            signingKey: PrivateKey?
        ): Signature? {
            if (signingKey == null) {
                return null
            }

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
            verificationKey: PublicKey?
        ): Signature? {
            if (verificationKey == null) {
                return null
            }

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

        @JvmStatic
        fun generateKeyPair(
            signatureKeyAlgorithm: String?,
            signatureProvider: String?,
            signatureKeySize: Int
        ): KeyPair {
            val keyPairGenerator: KeyPairGenerator = try {
                if (signatureProvider == null) {
                    KeyPairGenerator.getInstance(signatureKeyAlgorithm)
                } else {
                    KeyPairGenerator.getInstance(signatureKeyAlgorithm, signatureProvider)
                }
            } catch (e: GeneralSecurityException) {
                throw UncheckedSecurityException(cause = e)
            }
            keyPairGenerator.initialize(signatureKeySize)
            return keyPairGenerator.generateKeyPair()
        }

        @JvmStatic
        fun newBuilder(): Builder {
            return Builder()
        }

        @JvmStatic
        fun newMessageSigner(messageSigningProperties: MessageSigningProperties): MessageSigner {
            return Builder(messageSigningProperties).build()
        }
    }

    class Builder {
        private val properties: MessageSigningProperties

        internal constructor() {
            this.properties = MessageSigningProperties()
        }

        constructor(properties: MessageSigningProperties) {
            this.properties = properties
        }

        fun signingEnabled(signingEnabled: Boolean): Builder = this.apply { properties.signingEnabled = signingEnabled }

        fun stripAvroHeader(stripAvroHeader: Boolean): Builder =
            this.apply { properties.stripHeaders = stripAvroHeader }

        fun signatureAlgorithm(signatureAlgorithm: String): Builder =
            this.apply { properties.algorithm = signatureAlgorithm }

        fun signatureProvider(signatureProvider: String?): Builder =
            this.apply { properties.provider = signatureProvider }

        fun signatureKeyAlgorithm(signatureKeyAlgorithm: String): Builder =
            this.apply { properties.keyAlgorithm = signatureKeyAlgorithm }

        fun signatureKeySize(signatureKeySize: Int): Builder =
            this.apply { properties.keySize = signatureKeySize }

        fun signingKey(signingKey: PrivateKey?): Builder = this.apply { properties.privateKey = signingKey }

        fun signingKey(signingKeyPem: String?): Builder {
            if (signingKeyPem == null) {
                properties.privateKey = null
                return this
            }
            val base64 = PEM_REMOVAL_PATTERN.matcher(signingKeyPem).replaceAll("")
            val bytes = Base64.getDecoder().decode(base64)
            return this.signingKey(bytes)
        }

        fun signingKey(signingKeyBytes: ByteArray?): Builder {
            if (signingKeyBytes == null) {
                properties.privateKey = null
                return this
            }
            val keySpec = PKCS8EncodedKeySpec(signingKeyBytes)
            try {
                properties.privateKey =
                    KeyFactory.getInstance(properties.keyAlgorithm).generatePrivate(keySpec)
            } catch (e: GeneralSecurityException) {
                throw UncheckedSecurityException(cause = e)
            }
            return this
        }

        fun verificationKey(verificationKey: PublicKey?): Builder =
            this.apply { properties.publicKey = verificationKey }

        fun verificationKey(verificationKeyPem: String?): Builder {
            if (verificationKeyPem == null) {
                properties.publicKey = null
                return this
            }
            val base64 = PEM_REMOVAL_PATTERN.matcher(verificationKeyPem).replaceAll("")
            val bytes = Base64.getDecoder().decode(base64)
            return this.verificationKey(bytes)
        }

        fun verificationKey(verificationKeyBytes: ByteArray?): Builder {
            if (verificationKeyBytes == null) {
                properties.publicKey = null
                return this
            }
            val keySpec = X509EncodedKeySpec(verificationKeyBytes)
            try {
                properties.publicKey =
                    KeyFactory.getInstance(properties.keyAlgorithm).generatePublic(keySpec)
            } catch (e: GeneralSecurityException) {
                throw UncheckedSecurityException(cause = e)
            }
            return this
        }

        fun keyPair(keyPair: KeyPair): Builder = this.apply {
            properties.privateKey = keyPair.private
            properties.publicKey = keyPair.public
        }

        fun generateKeyPair(): Builder {
            return this.keyPair(
                generateKeyPair(
                    properties.keyAlgorithm,
                    properties.provider,
                    properties.keySize
                )
            )
        }

        fun build(): MessageSigner = MessageSigner(this.properties)

        companion object {
            private val PEM_REMOVAL_PATTERN: Pattern = Pattern.compile("-----(?:BEGIN|END) .*?-----|\\r|\\n")
        }
    }
}

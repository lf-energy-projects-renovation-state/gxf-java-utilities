// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package com.alliander.osgp.kafka.message.signing;

import com.alliander.osgp.kafka.message.wrapper.SignableMessageWrapper;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class MessageSigner {
    public static final String DEFAULT_SIGNATURE_ALGORITHM = "SHA256withRSA";
    public static final String DEFAULT_SIGNATURE_PROVIDER = "SunRsaSign";
    public static final String DEFAULT_SIGNATURE_KEY_ALGORITHM = "RSA";
    public static final int DEFAULT_SIGNATURE_KEY_SIZE = 2048;

    // Two magic bytes (0xC3, 0x01) followed by an 8-byte fingerprint
    public static final int AVRO_HEADER_LENGTH = 10;

    public static final String RECORD_HEADER_KEY_SIGNATURE = "signature";

    private final boolean signingEnabled;

    private boolean stripAvroHeader;

    private String signatureAlgorithm;
    private String signatureProvider;
    private String signatureKeyAlgorithm;
    private int signatureKeySize;

    private final Signature signingSignature;
    private final Signature verificationSignature;

    private PrivateKey signingKey;
    private PublicKey verificationKey;

    private MessageSigner(final MessageSigningProperties properties) {
        this.signingSignature =
                signatureInstance(
                        properties.getSignatureAlgorithm(), properties.getSignatureProvider(), properties.getSigningKey());
        this.verificationSignature =
                signatureInstance(
                        properties.getSignatureAlgorithm(), properties.getSignatureProvider(), properties.getVerificationKey());
        this.signingEnabled = properties.getSigningEnabled();
        if (!this.signingEnabled) {
            return;
        }
        this.stripAvroHeader = properties.getStripAvroHeader();
        this.signatureAlgorithm = properties.getSignatureAlgorithm();
        this.signatureKeyAlgorithm = properties.getSignatureKeyAlgorithm();
        this.signatureKeySize = properties.getSignatureKeySize();
        if (properties.getSigningKey() == null && properties.getVerificationKey() == null) {
            throw new IllegalArgumentException(
                    "A signing key (PrivateKey) or verification key (PublicKey) must be provided");
        }
        this.signingKey = properties.getSigningKey();
        this.verificationKey = properties.getVerificationKey();
        if (properties.getSignatureProvider() != null) {
            this.signatureProvider = properties.getSignatureProvider();
        } else if (this.signingSignature != null) {
            this.signatureProvider = this.signingSignature.getProvider().getName();
        } else if (this.verificationSignature != null) {
            this.signatureProvider = this.verificationSignature.getProvider().getName();
        } else {
            // Should not happen, set to null and ignore.
            this.signatureProvider = null;
        }
    }

    public boolean canSignMessages() {
        return this.signingEnabled && this.signingSignature != null;
    }

    /**
     * Signs the provided {@code message}, overwriting an existing signature, if a non-null value is
     * already set.
     *
     * @param message the message to be signed
     * @throws IllegalStateException      if this message signer has a public key for signature
     *                                    verification, but does not have the private key needed for signing messages.
     * @throws UncheckedIOException       if determining the bytes for the message throws an IOException.
     * @throws UncheckedSecurityException if the signing process throws a SignatureException.
     */
    public void sign(final SignableMessageWrapper<?> message) {
        if (this.signingEnabled) {
            final byte[] signatureBytes = this.signature(message);
            message.setSignature(ByteBuffer.wrap(signatureBytes));
        }
    }

    /**
     * Signs the provided {@code producerRecord} in the header, overwriting an existing signature, if a non-null value is
     * already set.
     *
     * @param producerRecord the record to be signed
     * @throws IllegalStateException      if this message signer has a public key for signature
     *                                    verification, but does not have the private key needed for signing messages.
     * @throws UncheckedIOException       if determining the bytes for the message throws an IOException.
     * @throws UncheckedSecurityException if the signing process throws a SignatureException.
     */
    public void sign(final ProducerRecord<String, ? extends SpecificRecordBase> producerRecord) {
        if (this.signingEnabled) {
            final byte[] signature = this.signature(producerRecord);
            producerRecord.headers().add(RECORD_HEADER_KEY_SIGNATURE, signature);
        }
    }

    /**
     * Determines the signature for the given {@code message}.
     *
     * <p>The value for the signature in the message will be set to {@code null} to properly determine
     * the signature, but is restored to its original value before this method returns.
     *
     * @param message the message to be signed
     * @return the signature for the message
     * @throws IllegalStateException      if this message signer has a public key for signature
     *                                    verification, but does not have the private key needed for signing messages.
     * @throws UncheckedIOException       if determining the bytes for the message throws an IOException.
     * @throws UncheckedSecurityException if the signing process throws a SignatureException.
     */
    public byte[] signature(final SignableMessageWrapper<?> message) {
        if (!this.canSignMessages()) {
            throw new IllegalStateException(
                    "This MessageSigner is not configured for signing, it can only be used for verification");
        }
        final ByteBuffer oldSignature = message.getSignature();
        try {
            message.setSignature(null);
            synchronized (this.signingSignature) {
                final byte[] messageBytes;
                if (this.stripAvroHeader) {
                    messageBytes = this.stripAvroHeader(this.toByteBuffer(message));
                } else {
                    messageBytes = this.toByteBuffer(message).array();
                }
                this.signingSignature.update(messageBytes);
                return this.signingSignature.sign();
            }
        } catch (final SignatureException e) {
            throw new UncheckedSecurityException("Unable to sign message", e);
        } finally {
            message.setSignature(oldSignature);
        }
    }

    /**
     * Determines the signature for the given {@code producerRecord}.
     *
     * <p>The value for the signature in the record will be set to {@code null} to properly determine
     * the signature, but is restored to its original value before this method returns.
     *
     * @param producerRecord the record to be signed
     * @return the signature for the record
     * @throws IllegalStateException      if this message signer has a public key for signature
     *                                    verification, but does not have the private key needed for signing messages.
     * @throws UncheckedIOException       if determining the bytes throws an IOException.
     * @throws UncheckedSecurityException if the signing process throws a SignatureException.
     */
    public byte[] signature(final ProducerRecord<String, ? extends SpecificRecordBase> producerRecord) {
        if (!this.canSignMessages()) {
            throw new IllegalStateException(
                    "This MessageSigner is not configured for signing, it can only be used for verification");
        }
        final Header oldSignatureHeader = producerRecord.headers().lastHeader(RECORD_HEADER_KEY_SIGNATURE);
        try {
            producerRecord.headers().remove(RECORD_HEADER_KEY_SIGNATURE);
            synchronized (this.signingSignature) {
                final byte[] messageBytes;
                final SpecificRecordBase specificRecordBase = producerRecord.value();
                if (this.stripAvroHeader) {
                    messageBytes = this.stripAvroHeader(this.toByteBuffer(specificRecordBase));
                } else {
                    messageBytes = this.toByteBuffer(specificRecordBase).array();
                }
                this.signingSignature.update(messageBytes);
                return this.signingSignature.sign();
            }
        } catch (final SignatureException e) {
            throw new UncheckedSecurityException("Unable to sign message", e);
        } finally {
            if (oldSignatureHeader != null) {
                producerRecord.headers().add(RECORD_HEADER_KEY_SIGNATURE, oldSignatureHeader.value());
            }
        }
    }

    public boolean canVerifyMessageSignatures() {
        return this.signingEnabled && this.verificationSignature != null;
    }

    /**
     * Verifies the signature of the provided {@code message}.
     *
     * @param message the message to be verified
     * @return {@code true} if the signature of the given {@code message} was verified; {@code false}
     * if not.
     * @throws IllegalStateException      if this message signer has a private key needed for signing
     *                                    messages, but does not have the public key for signature verification.
     * @throws UncheckedIOException       if determining the bytes for the message throws an IOException.
     * @throws UncheckedSecurityException if the signature verification process throws a
     *                                    SignatureException.
     */
    public boolean verify(final SignableMessageWrapper<?> message) {
        if (!this.canVerifyMessageSignatures()) {
            throw new IllegalStateException(
                    "This MessageSigner is not configured for verification, it can only be used for signing");
        }

        final ByteBuffer messageSignature = message.getSignature();
        if (messageSignature == null) {
            return false;
        }
        messageSignature.mark();
        final byte[] signatureBytes = new byte[messageSignature.remaining()];
        messageSignature.get(signatureBytes);

        try {
            message.setSignature(null);
            synchronized (this.verificationSignature) {
                return this.verifySignatureBytes(signatureBytes, this.toByteBuffer(message));
            }
        } catch (final SignatureException e) {
            throw new UncheckedSecurityException("Unable to verify message signature", e);
        } finally {
            messageSignature.reset();
            message.setSignature(messageSignature);
        }
    }

    /**
     * Verifies the signature of the provided {@code consumerRecord}.
     *
     * @param consumerRecord the record to be verified
     * @return {@code true} if the signature of the given {@code consumerRecord} was verified; {@code false}
     * if not.
     * @throws IllegalStateException      if this message signer has a private key needed for signing
     *                                    messages, but does not have the public key for signature verification.
     * @throws UncheckedIOException       if determining the bytes throws an IOException.
     * @throws UncheckedSecurityException if the signature verification process throws a
     *                                    SignatureException.
     */
    public boolean verify(final ConsumerRecord<String, ? extends SpecificRecordBase> consumerRecord) {
        if (!this.canVerifyMessageSignatures()) {
            throw new IllegalStateException(
                    "This MessageSigner is not configured for verification, it can only be used for signing");
        }

        final Header header = consumerRecord.headers().lastHeader(RECORD_HEADER_KEY_SIGNATURE);
        if (header == null) {
            throw new IllegalStateException(
                    "This ProducerRecord does not contain a signature header");
        }
        final byte[] signatureBytes = header.value();
        if (signatureBytes == null || signatureBytes.length == 0) {
            return false;
        }

        try {
            consumerRecord.headers().remove(RECORD_HEADER_KEY_SIGNATURE);
            synchronized (this.verificationSignature) {
                final SpecificRecordBase specificRecordBase = consumerRecord.value();
                return this.verifySignatureBytes(signatureBytes, this.toByteBuffer(specificRecordBase));
            }
        } catch (final SignatureException e) {
            throw new UncheckedSecurityException("Unable to verify message signature", e);
        }
    }

    private boolean verifySignatureBytes(final byte[] signatureBytes, final ByteBuffer messageByteBuffer) throws SignatureException {
        final byte[] messageBytes;
        if (this.stripAvroHeader) {
            messageBytes = this.stripAvroHeader(messageByteBuffer);
        } else {
            messageBytes = messageByteBuffer.array();
        }
        this.verificationSignature.update(messageBytes);
        return this.verificationSignature.verify(signatureBytes);
    }

    private boolean hasAvroHeader(final byte[] bytes) {
        return bytes.length >= AVRO_HEADER_LENGTH
                && (bytes[0] & 0xFF) == 0xC3
                && (bytes[1] & 0xFF) == 0x01;
    }

    private byte[] stripAvroHeader(final ByteBuffer byteBuffer) {
        final byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        if (this.hasAvroHeader(bytes)) {
            return Arrays.copyOfRange(bytes, AVRO_HEADER_LENGTH, bytes.length);
        }
        return bytes;
    }

    private ByteBuffer toByteBuffer(final SignableMessageWrapper<?> message) {
        try {
            return message.toByteBuffer();
        } catch (final IOException e) {
            throw new UncheckedIOException("Unable to determine ByteBuffer for Message", e);
        }
    }

    private ByteBuffer toByteBuffer(final SpecificRecordBase message) {
        try {
            return new BinaryMessageEncoder<>(message.getSpecificData(), message.getSchema()).encode(message);
        } catch (final IOException e) {
            throw new UncheckedIOException("Unable to determine ByteBuffer for Message", e);
        }
    }

    public boolean isSigningEnabled() {
        return this.signingEnabled;
    }

    public Optional<PrivateKey> signingKey() {
        return Optional.ofNullable(this.signingKey);
    }

    public Optional<String> signingKeyPem() {
        return this.signingKey().map(key -> this.keyAsMem(key, key.getAlgorithm() + " PRIVATE KEY"));
    }

    public Optional<PublicKey> verificationKey() {
        return Optional.ofNullable(this.verificationKey);
    }

    public Optional<String> verificationKeyPem() {
        return this.verificationKey()
                .map(key -> this.keyAsMem(key, key.getAlgorithm() + " PUBLIC KEY"));
    }

    private String keyAsMem(final Key key, final String label) {
        return "-----BEGIN " + label + "-----" + "\r\n"
                + Base64.getMimeEncoder().encodeToString(key.getEncoded()) + "\r\n"
                + "-----END " + label + "-----" + "\r\n";
    }

    @Override
    public String toString() {
        return String.format(
                "MessageSigner[algorithm=\"%s\"-\"%s\", provider=\"%s\", keySize=%d, sign=%b, verify=%b]",
                this.signatureAlgorithm,
                this.signatureKeyAlgorithm,
                this.signatureProvider,
                this.signatureKeySize,
                this.canSignMessages(),
                this.canVerifyMessageSignatures());
    }

    public String descriptionWithKeys() {
        final StringBuilder sb = new StringBuilder(this.toString());
        this.signingKeyPem().ifPresent(key -> sb.append(System.lineSeparator()).append(key));
        this.verificationKeyPem().ifPresent(key -> sb.append(System.lineSeparator()).append(key));
        return sb.toString();
    }

    private static Signature signatureInstance(
            final String signatureAlgorithm,
            final String signatureProvider,
            final PrivateKey signingKey) {

        if (signingKey == null) {
            return null;
        }

        final Signature signature = signatureInstance(signatureAlgorithm, signatureProvider);
        try {
            signature.initSign(signingKey);
        } catch (final InvalidKeyException e) {
            throw new UncheckedSecurityException(e);
        }
        return signature;
    }

    private static Signature signatureInstance(
            final String signatureAlgorithm,
            final String signatureProvider,
            final PublicKey verificationKey) {

        if (verificationKey == null) {
            return null;
        }

        final Signature signature = signatureInstance(signatureAlgorithm, signatureProvider);
        try {
            signature.initVerify(verificationKey);
        } catch (final InvalidKeyException e) {
            throw new UncheckedSecurityException(e);
        }
        return signature;
    }

    private static Signature signatureInstance(
            final String signatureAlgorithm, final String signatureProvider) {
        try {
            if (signatureProvider == null) {
                return Signature.getInstance(signatureAlgorithm);
            }
            return Signature.getInstance(signatureAlgorithm, signatureProvider);
        } catch (final GeneralSecurityException e) {
            throw new UncheckedSecurityException("Unable to create Signature for Avro Messages", e);
        }
    }

    public static KeyPair generateKeyPair(
            final String signatureKeyAlgorithm,
            final String signatureProvider,
            final int signatureKeySize) {
        final KeyPairGenerator keyPairGenerator;
        try {
            if (signatureProvider == null) {
                keyPairGenerator = KeyPairGenerator.getInstance(signatureKeyAlgorithm);
            } else {
                keyPairGenerator = KeyPairGenerator.getInstance(signatureKeyAlgorithm, signatureProvider);
            }
        } catch (final GeneralSecurityException e) {
            throw new UncheckedSecurityException(e);
        }
        keyPairGenerator.initialize(signatureKeySize);
        return keyPairGenerator.generateKeyPair();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static MessageSigner newMessageSigner(final MessageSigningProperties messageSigningProperties) {
        return new Builder(messageSigningProperties).build();
    }

    public static final class Builder {

        private static final Pattern PEM_REMOVAL_PATTERN =
                Pattern.compile("-----(?:BEGIN|END) .*?-----|\\r|\\n");

        private final MessageSigningProperties properties;

        private Builder() {
            this.properties = new MessageSigningProperties();
        }
        private Builder(final MessageSigningProperties properties) {
            this.properties = properties;
        }

        public Builder signingEnabled(final boolean signingEnabled) {
            this.properties.setSigningEnabled(signingEnabled);
            return this;
        }

        public Builder stripAvroHeader(final boolean stripAvroHeader) {
            this.properties.setStripAvroHeader(stripAvroHeader);
            return this;
        }

        public Builder signatureAlgorithm(final String signatureAlgorithm) {
            this.properties.setSignatureAlgorithm(Objects.requireNonNull(signatureAlgorithm));
            return this;
        }

        public Builder signatureProvider(final String signatureProvider) {
            this.properties.setSignatureProvider(signatureProvider);
            return this;
        }

        public Builder signatureKeyAlgorithm(final String signatureKeyAlgorithm) {
            this.properties.setSignatureKeyAlgorithm(Objects.requireNonNull(signatureKeyAlgorithm));
            return this;
        }

        public Builder signatureKeySize(final int signatureKeySize) {
            this.properties.setSignatureKeySize(signatureKeySize);
            return this;
        }

        public Builder signingKey(final PrivateKey signingKey) {
            this.properties.setSigningKey(signingKey);
            return this;
        }

        public Builder signingKey(final String signingKeyPem) {
            if (signingKeyPem == null) {
                this.properties.setSigningKey(null);
                return this;
            }
            final String base64 = PEM_REMOVAL_PATTERN.matcher(signingKeyPem).replaceAll("");
            final byte[] bytes = Base64.getDecoder().decode(base64);
            return this.signingKey(bytes);
        }

        public Builder signingKey(final byte[] signingKeyBytes) {
            if (signingKeyBytes == null) {
                this.properties.setSigningKey(null);
                return this;
            }
            final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(signingKeyBytes);
            try {
                this.properties.setSigningKey(KeyFactory.getInstance(this.properties.getSignatureKeyAlgorithm()).generatePrivate(keySpec));
            } catch (final GeneralSecurityException e) {
                throw new UncheckedSecurityException(e);
            }
            return this;
        }

        public Builder verificationKey(final PublicKey verificationKey) {
            this.properties.setVerificationKey(verificationKey);
            return this;
        }

        public Builder verificationKey(final String verificationKeyPem) {
            if (verificationKeyPem == null) {
                this.properties.setVerificationKey(null);
                return this;
            }
            final String base64 = PEM_REMOVAL_PATTERN.matcher(verificationKeyPem).replaceAll("");
            final byte[] bytes = Base64.getDecoder().decode(base64);
            return this.verificationKey(bytes);
        }

        public Builder verificationKey(final byte[] verificationKeyBytes) {
            if (verificationKeyBytes == null) {
                this.properties.setVerificationKey(null);
                return this;
            }
            final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(verificationKeyBytes);
            try {
                this.properties.setVerificationKey(KeyFactory.getInstance(this.properties.getSignatureKeyAlgorithm()).generatePublic(keySpec));
            } catch (final GeneralSecurityException e) {
                throw new UncheckedSecurityException(e);
            }
            return this;
        }

        public Builder keyPair(final KeyPair keyPair) {
            this.properties.setSigningKey(keyPair.getPrivate());
            this.properties.setVerificationKey(keyPair.getPublic());
            return this;
        }

        public Builder generateKeyPair() {
            return this.keyPair(
                    MessageSigner.generateKeyPair(
                            this.properties.getSignatureKeyAlgorithm(), this.properties.getSignatureProvider(), this.properties.getSignatureKeySize()));
        }

        public MessageSigner build() {
            return new MessageSigner(this.properties);
        }
    }
}
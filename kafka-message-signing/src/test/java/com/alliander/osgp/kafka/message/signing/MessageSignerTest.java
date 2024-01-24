// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package com.alliander.osgp.kafka.message.signing;

import static org.assertj.core.api.Assertions.assertThat;

import com.alliander.osgp.kafka.message.wrapper.SignableMessageWrapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MessageSignerTest {

  private static final boolean SIGNING_ENABLED = true;

  private static final boolean STRIP_HEADERS = true;

  private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
  private static final String SIGNATURE_PROVIDER = "SunRsaSign";
  private static final String SIGNATURE_KEY_ALGORITHM = "RSA";
  private static final int SIGNATURE_KEY_SIZE = 2048;
  private static final int SIGNATURE_KEY_SIZE_BYTES = SIGNATURE_KEY_SIZE / 8;

  private static final KeyPair KEY_PAIR =
      MessageSigner.generateKeyPair(
          SIGNATURE_KEY_ALGORITHM, SIGNATURE_PROVIDER, SIGNATURE_KEY_SIZE);

  private static final Random RANDOM = new SecureRandom();

  private final MessageSigner messageSigner =
      MessageSigner.newBuilder()
          .signingEnabled(SIGNING_ENABLED)
          .stripHeaders(STRIP_HEADERS)
          .signatureAlgorithm(SIGNATURE_ALGORITHM)
          .signatureProvider(SIGNATURE_PROVIDER)
          .signatureKeyAlgorithm(SIGNATURE_KEY_ALGORITHM)
          .signatureKeySize(SIGNATURE_KEY_SIZE)
          .keyPair(KEY_PAIR)
          .build();

  @Test
  void signsMessageWithoutSignature() {
    final SignableMessageWrapper<?> messageWrapper = this.messageWrapper();

    this.messageSigner.sign(messageWrapper);

    assertThat(messageWrapper.getSignature()).isNotNull();
  }

  @Test
  void signsMessageReplacingSignature() {
    final byte[] randomSignature = this.randomSignature();
    final TestableWrapper messageWrapper = this.messageWrapper();

    this.messageSigner.sign(messageWrapper);

    final byte[] actualSignature = this.bytes(messageWrapper.getSignature());
    assertThat(actualSignature).isNotNull().isNotEqualTo(randomSignature);
  }

  @Test
  void verifiesMessagesWithValidSignature() {
    final TestableWrapper message = this.properlySignedMessage();

    final boolean signatureWasVerified = this.messageSigner.verify(message);

    assertThat(signatureWasVerified).isTrue();
  }

  @Test
  void doesNotVerifyMessagesWithoutSignature() {
    final TestableWrapper messageWrapper = this.messageWrapper();

    final boolean signatureWasVerified = this.messageSigner.verify(messageWrapper);

    assertThat(signatureWasVerified).isFalse();
  }

  @Test
  void doesNotVerifyMessagesWithIncorrectSignature() {
    final byte[] randomSignature = this.randomSignature();
    final TestableWrapper messageWrapper = this.messageWrapper(randomSignature);

    final boolean signatureWasVerified = this.messageSigner.verify(messageWrapper);

    assertThat(signatureWasVerified).isFalse();
  }

  @Test
  void verifiesMessagesPreservingTheSignatureAndItsProperties() {
    final TestableWrapper message = this.properlySignedMessage();
    final ByteBuffer originalSignature = message.getSignature();
    final int originalPosition = originalSignature.position();
    final int originalLimit = originalSignature.limit();
    final int originalRemaining = originalSignature.remaining();

    this.messageSigner.verify(message);

    final ByteBuffer verifiedSignature = message.getSignature();
    assertThat(verifiedSignature).isEqualTo(originalSignature);
    assertThat(verifiedSignature.position()).isEqualTo(originalPosition);
    assertThat(verifiedSignature.limit()).isEqualTo(originalLimit);
    assertThat(verifiedSignature.remaining()).isEqualTo(originalRemaining);
  }

  private String fromPemResource(final String name) {
    return new BufferedReader(
            new InputStreamReader(
                this.getClass().getResourceAsStream(name), StandardCharsets.ISO_8859_1))
        .lines()
        .collect(Collectors.joining(System.lineSeparator()));
  }

  @Test
  void worksWithKeysFromPemEncodedResources() {

    final MessageSigner messageSignerWithKeysFromResources =
        MessageSigner.newBuilder()
            .signingEnabled(SIGNING_ENABLED)
            .signatureAlgorithm(SIGNATURE_ALGORITHM)
            .signatureProvider(SIGNATURE_PROVIDER)
            .signatureKeyAlgorithm(SIGNATURE_KEY_ALGORITHM)
            .signatureKeySize(SIGNATURE_KEY_SIZE)
            .signingKey(this.fromPemResource("/rsa-private.pem"))
            .verificationKey(this.fromPemResource("/rsa-public.pem"))
            .build();

    final TestableWrapper messageWrapper = this.messageWrapper();
    messageSignerWithKeysFromResources.sign(messageWrapper);
    final boolean signatureWasVerified = messageSignerWithKeysFromResources.verify(messageWrapper);

    assertThat(signatureWasVerified).isTrue();
  }

  @Test
  void signingCanBeDisabled() {
    final MessageSigner messageSignerSigningDisabled =
        MessageSigner.newBuilder().signingEnabled(!SIGNING_ENABLED).build();

    assertThat(messageSignerSigningDisabled.canSignMessages()).isFalse();
    assertThat(messageSignerSigningDisabled.canVerifyMessageSignatures()).isFalse();
  }

  private TestableWrapper messageWrapper() {
    return new TestableWrapper();
  }

  private TestableWrapper messageWrapper(final byte[] signature) {
    final TestableWrapper testableWrapper = new TestableWrapper();
    testableWrapper.setSignature(ByteBuffer.wrap(signature));
    return testableWrapper;
  }

  private TestableWrapper properlySignedMessage() {
    final TestableWrapper messageWrapper = this.messageWrapper();
    this.messageSigner.sign(messageWrapper);
    return messageWrapper;
  }

  private byte[] randomSignature() {
    final byte[] signature = new byte[SIGNATURE_KEY_SIZE_BYTES];
    RANDOM.nextBytes(signature);
    return signature;
  }

  private byte[] bytes(final ByteBuffer byteBuffer) {
    if (byteBuffer == null) {
      return null;
    }
    final byte[] bytes = new byte[byteBuffer.remaining()];
    byteBuffer.get(bytes);
    return bytes;
  }

  private static class TestableWrapper extends SignableMessageWrapper<String> {
    private ByteBuffer signature;

    protected TestableWrapper() {
      super("Some test message");
    }

    @Override
    public ByteBuffer toByteBuffer() throws IOException {
      return ByteBuffer.wrap(this.message.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public ByteBuffer getSignature() {
      return this.signature;
    }

    @Override
    public void setSignature(final ByteBuffer signature) {
      this.signature = signature;
    }
  }
}

package com.alliander.osgp.kafka.message.signing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

@Configuration
public class MessageSigningAutoConfiguration {
  public static final String SIGNING_KEY_NAME = "private signing key";

  public static final String VERIFICATION_KEY_NAME = "public verification key";

  @Value("${message-signing.enabled}")
  private boolean signingEnabled;

  @Value("${message-signing.strip-headers}")
  private boolean stripHeaders;

  @Value("${message-signing.signature.algorithm:SHA256withRSA}")
  private String signatureAlgorithm;

  @Value("${message-signing.signature.provider:SunRsaSign}")
  private String signatureProvider;

  @Value("${message-signing.signature.key.algorithm:RSA}")
  private String signatureKeyAlgorithm;

  @Value("${message-signing.signature.key.size:2048}")
  private int signatureKeySize;

  @Value("${message-signing.signature.key.private:#{null}}")
  private String signingKeyResource;

  @Value("${message-signing.signature.key.public:#{null}}")
  private String verificationKeyResource;

  @Bean
  public MessageSigner messageSigner() {
    if(this.signingEnabled) {
      return MessageSigner.newBuilder()
          .signingEnabled(this.signingEnabled)
          .stripAvroHeader(this.stripHeaders)
          .signatureAlgorithm(this.signatureAlgorithm)
          .signatureProvider(this.signatureProvider)
          .signatureKeyAlgorithm(this.signatureKeyAlgorithm)
          .signatureKeySize(this.signatureKeySize)
          .signingKey(this.fromPemResource(this.signingKeyResource))
          .verificationKey(this.fromPemResource(this.verificationKeyResource))
          .build();
    } else {
      return MessageSigner.newBuilder()
          .signingEnabled(false)
          .build();
    }
  }

  private String readKeyFromPemResource(final Resource keyResource, final String name) {
    if (keyResource == null) {
      return null;
    }
    try {
      return StreamUtils.copyToString(keyResource.getInputStream(), StandardCharsets.ISO_8859_1);
    } catch (final IOException e) {
      throw new UncheckedIOException("Unable to read " + name + " as ISO-LATIN-1 PEM text", e);
    }
  }

  private String fromPemResource(final String name) {
    var thisClass = this.getClass();
    var stream = thisClass.getResourceAsStream(name);
    return new BufferedReader(
        new InputStreamReader(
            stream, StandardCharsets.ISO_8859_1))
        .lines()
        .collect(Collectors.joining(System.lineSeparator()));
  }
}

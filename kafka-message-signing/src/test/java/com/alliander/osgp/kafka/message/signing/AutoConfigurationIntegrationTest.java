package com.alliander.osgp.kafka.message.signing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = MessageSigningAutoConfiguration.class)
@EnableAutoConfiguration
@TestPropertySource("classpath:/application.yaml")
class AutoConfigurationIntegrationTest {

  @Autowired private MessageSigner messageSigner;

  @Test
  void autoConfigurationIntegrationTest() {
    assertTrue(this.messageSigner.isSigningEnabled());
    assertTrue(this.messageSigner.canSignMessages());
    assertTrue(this.messageSigner.canVerifyMessageSignatures());
    assertNotNull(this.messageSigner.signingKey().get());
    assertNotNull(this.messageSigner.verificationKey().get());
  }
}

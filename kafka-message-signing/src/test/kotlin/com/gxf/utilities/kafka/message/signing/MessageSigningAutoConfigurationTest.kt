package com.gxf.utilities.kafka.message.signing

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest(classes = [MessageSigningAutoConfiguration::class])
@EnableAutoConfiguration
@EnableConfigurationProperties(MessageSigningProperties::class)
@TestPropertySource("classpath:/application.yaml")
class MessageSigningAutoConfigurationTest {
    @Autowired
    private lateinit var messageSigner: MessageSigner

    @Test
    fun autoConfigurationIntegrationTest() {
        assertTrue(messageSigner.isSigningEnabled())
        assertTrue(messageSigner.canSignMessages())
        assertTrue(messageSigner.canVerifyMessageSignatures())
        assertNotNull(messageSigner.signingKey().orElseThrow { AssertionError() })
        assertNotNull(messageSigner.verificationKey().orElseThrow { AssertionError() })
    }
}

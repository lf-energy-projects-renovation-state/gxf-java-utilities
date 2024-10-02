// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing

import org.assertj.core.api.Assertions.assertThat
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
    @Autowired private lateinit var messageSigner: MessageSigner

    @Test
    fun autoConfigurationIntegrationTest() {
        assertThat(messageSigner.signingEnabled).isTrue()
        assertThat(messageSigner.canSignMessages()).isTrue()
        assertThat(messageSigner.canVerifyMessageSignatures()).isTrue()
    }
}

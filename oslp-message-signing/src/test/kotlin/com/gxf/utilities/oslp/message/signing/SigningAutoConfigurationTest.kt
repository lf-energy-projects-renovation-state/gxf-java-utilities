// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.oslp.message.signing

import com.gxf.utilities.oslp.message.signing.configuration.SigningAutoConfiguration
import com.gxf.utilities.oslp.message.signing.configuration.SigningProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest(classes = [SigningAutoConfiguration::class])
@EnableAutoConfiguration
@EnableConfigurationProperties(SigningProperties::class)
@TestPropertySource("classpath:/application.yaml")
class SigningAutoConfigurationTest {
    @Autowired private lateinit var signingUtil: SigningUtil
    @MockitoBean private lateinit var keyProvider: KeyProvider

    @Test
    fun autoConfigurationIntegrationTest() {
        assertThat(signingUtil.signingConfiguration.securityProvider).isEqualTo("security-provider")
        assertThat(signingUtil.signingConfiguration.securityAlgorithm).isEqualTo("security-algorithm")
    }
}

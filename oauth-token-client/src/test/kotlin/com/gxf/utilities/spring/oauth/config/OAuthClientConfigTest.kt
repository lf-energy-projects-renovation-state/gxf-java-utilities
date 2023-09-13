/*
SPDX-FileCopyrightText: Contributors to the GXF project

SPDX-License-Identifier: Apache-2.0
*/
package com.gxf.utilities.spring.oauth.config

import com.gxf.utilities.spring.oauth.exceptions.OAuthTokenException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource


internal class OAuthClientConfigTest {

    @Test
    fun `should read private key`() {
        val client = OAuthClientConfig()
        val privateKey = client.getPrivateKey(ClassPathResource("keys/private-key.key"))
        assertThat(privateKey).isNotNull()
    }

    @Test
    fun `should throw exception for non existent private key`() {
        val client = OAuthClientConfig()
        assertThatThrownBy { client.getPrivateKey(ClassPathResource("keys/does-not-exist.key"))}
            .isInstanceOf(OAuthTokenException::class.java)
            .hasMessage("Error getting private key")
    }

    @Test
    fun `should read certificate`() {
        val client = OAuthClientConfig()
        val certificate = client.getCertificate(ClassPathResource("keys/certificate.crt"))
        assertThat(certificate).isNotNull()
    }

    @Test
    fun `should throw exception for non existent certificate`() {
        val client = OAuthClientConfig()

        assertThatThrownBy { client.getCertificate(ClassPathResource("keys/does-not-exist.key"))}
            .isInstanceOf(OAuthTokenException::class.java)
            .hasMessage("Error getting certificate")
    }

}

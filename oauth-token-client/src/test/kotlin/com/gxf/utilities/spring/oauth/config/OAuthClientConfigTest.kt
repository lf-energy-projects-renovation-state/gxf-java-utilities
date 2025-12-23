// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.spring.oauth.config

import com.gxf.utilities.spring.oauth.exceptions.OAuthTokenException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ClassPathResource

internal class OAuthClientConfigTest {

    @Test
    fun `should read private key`() {
        val client = MsalClientConfig()
        val privateKey = client.getPrivateKey(ClassPathResource("keys/private-key.key"))
        assertThat(privateKey).isNotNull()
    }

    @Test
    fun `should read private key with whitespace`() {
        val client = MsalClientConfig()
        val privateKey = client.getPrivateKey(ClassPathResource("keys/private-key-whitespace.key"))
        assertThat(privateKey).isNotNull()
    }

    @Test
    fun `should read base 64 private key`() {
        val client = MsalClientConfig()
        val privateKey =
            client.getPrivateKey(
                ByteArrayResource(ClassPathResource("keys/private-key.key").inputStream.readAllBytes())
            )
        assertThat(privateKey).isNotNull()
    }

    @Test
    fun `should throw exception for non existent private key`() {
        val client = MsalClientConfig()
        assertThatThrownBy { client.getPrivateKey(ClassPathResource("keys/does-not-exist.key")) }
            .isInstanceOf(OAuthTokenException::class.java)
            .hasMessage("Error getting private key")
    }

    @Test
    fun `should read certificate`() {
        val client = MsalClientConfig()
        val certificate = client.getCertificate(ClassPathResource("keys/certificate.crt"))
        assertThat(certificate).isNotNull()
    }

    @Test
    fun `should read certificate with whitespace`() {
        val client = MsalClientConfig()
        val certificate = client.getCertificate(ClassPathResource("keys/certificate-whitespace.crt"))
        assertThat(certificate).isNotNull()
    }

    @Test
    fun `should read base 64 resource`() {
        val client = MsalClientConfig()
        val certificate =
            client.getCertificate(
                ByteArrayResource(ClassPathResource("keys/certificate.crt").inputStream.readAllBytes())
            )
        assertThat(certificate).isNotNull()
    }

    @Test
    fun `should throw exception for non existent certificate`() {
        val client = MsalClientConfig()

        assertThatThrownBy { client.getCertificate(ClassPathResource("keys/does-not-exist.key")) }
            .isInstanceOf(OAuthTokenException::class.java)
            .hasMessage("Error getting certificate")
    }
}

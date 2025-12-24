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

internal class MsalClientConfigTest {

    private val client = MsalClientConfig()

    private val msalProperties =
        OAuthClientProperties(
            tokenLocation = null,
            clientId = "some-test-client-id",
            scope = "some-test-scope",
            tokenEndpoint = "https://localhost:56788/token",
            certificate = ClassPathResource("keys/certificate.crt"),
            privateKey = ClassPathResource("keys/private-key.key"),
        )

    @Test
    fun `should create confidential client application`() {
        val confidentialClientApplication = client.confidentialClientApplication(msalProperties)
        assertThat(confidentialClientApplication).isNotNull()
        assertThat(confidentialClientApplication.accounts)
    }

    @Test
    fun `should throw exception for invalid authority`() {
        val invalidTokenEndpointProperties = msalProperties.copy(tokenEndpoint = "ht://localhost:56788")
        assertThatThrownBy { client.confidentialClientApplication(invalidTokenEndpointProperties) }
            .isInstanceOf(OAuthTokenException::class.java)
            .hasMessage("Error creating client credentials")
    }

    @Test
    fun `should read private key`() {
        val privateKey = client.getPrivateKey(ClassPathResource("keys/private-key.key"))
        assertThat(privateKey).isNotNull()
    }

    @Test
    fun `should read private key with whitespace`() {
        val privateKey = client.getPrivateKey(ClassPathResource("keys/private-key-whitespace.key"))
        assertThat(privateKey).isNotNull()
    }

    @Test
    fun `should read base 64 private key`() {
        val privateKey =
            client.getPrivateKey(
                ByteArrayResource(ClassPathResource("keys/private-key.key").inputStream.readAllBytes())
            )
        assertThat(privateKey).isNotNull()
    }

    @Test
    fun `should throw exception for null private key`() {
        assertThatThrownBy { client.getPrivateKey(null) }
            .isInstanceOf(OAuthTokenException::class.java)
            .hasMessage("No private key provided")
    }

    @Test
    fun `should throw exception for non existent private key`() {
        assertThatThrownBy { client.getPrivateKey(ClassPathResource("keys/does-not-exist.key")) }
            .isInstanceOf(OAuthTokenException::class.java)
            .hasMessage("Private key class path resource [keys/does-not-exist.key] is not readable")
    }

    @Test
    fun `should read certificate`() {
        val certificate = client.getCertificate(ClassPathResource("keys/certificate.crt"))
        assertThat(certificate).isNotNull()
    }

    @Test
    fun `should read certificate with whitespace`() {
        val certificate = client.getCertificate(ClassPathResource("keys/certificate-whitespace.crt"))
        assertThat(certificate).isNotNull()
    }

    @Test
    fun `should read base 64 resource`() {
        val certificate =
            client.getCertificate(
                ByteArrayResource(ClassPathResource("keys/certificate.crt").inputStream.readAllBytes())
            )
        assertThat(certificate).isNotNull()
    }

    @Test
    fun `should throw exception for null certificate`() {
        assertThatThrownBy { client.getCertificate(null) }
            .isInstanceOf(OAuthTokenException::class.java)
            .hasMessage("No certificate provided")
    }

    @Test
    fun `should throw exception for non existent certificate`() {
        assertThatThrownBy { client.getCertificate(ClassPathResource("keys/does-not-exist.key")) }
            .isInstanceOf(OAuthTokenException::class.java)
            .hasMessage("Certificate class path resource [keys/does-not-exist.key] is not readable")
    }
}

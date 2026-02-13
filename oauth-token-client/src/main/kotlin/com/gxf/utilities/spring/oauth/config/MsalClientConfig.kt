// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.spring.oauth.config

import com.gxf.utilities.spring.oauth.config.condition.OAuthMsalEnabledCondition
import com.gxf.utilities.spring.oauth.exceptions.OAuthTokenException
import com.microsoft.aad.msal4j.ClientCredentialFactory
import com.microsoft.aad.msal4j.ClientCredentialParameters
import com.microsoft.aad.msal4j.ConfidentialClientApplication
import com.microsoft.aad.msal4j.IClientCredential
import io.github.oshai.kotlinlogging.KotlinLogging
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.concurrent.Callable
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource

@Configuration
@Conditional(OAuthMsalEnabledCondition::class)
class MsalClientConfig {

    companion object {
        private val PEM_REMOVAL_PATTERN = Regex("-----[A-Z ]*-----")
    }

    private val logger = KotlinLogging.logger {}

    @Bean
    fun clientCredentialParameters(properties: OAuthClientProperties): ClientCredentialParameters =
        ClientCredentialParameters.builder(setOf(properties.scope)).build()

    @Bean
    fun confidentialClientApplication(properties: OAuthClientProperties): ConfidentialClientApplication {
        val credential: IClientCredential =
            if (properties.clientAssertion != null) {
                logger.info { "Using client assertion for msal credentials" }
                ClientCredentialFactory.createFromCallback(getClientAssertionCallable(properties.clientAssertion))
            } else {
                logger.info { "Using certificates for msal credentials" }
                ClientCredentialFactory.createFromCertificate(
                    getPrivateKey(properties.privateKey),
                    getCertificate(properties.certificate),
                )
            }
        return try {
            ConfidentialClientApplication.builder(properties.clientId, credential)
                .authority(properties.tokenEndpoint)
                .build()
        } catch (e: Exception) {
            throw OAuthTokenException("Error creating client credentials", e)
        }
    }

    /** Reads a private key file and puts */
    fun getPrivateKey(resource: Resource?): PrivateKey {
        if (resource == null) {
            throw OAuthTokenException("No private key provided")
        } else if (!resource.isReadable) {
            throw OAuthTokenException("Private key ${resource.description} is not readable")
        }

        try {
            logger.info { "Reading private key: ${resource.description}" }
            val privateKeyContent = readPEMFile(resource)
            val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent))
            return KeyFactory.getInstance("RSA").generatePrivate(keySpecPKCS8)
        } catch (e: Exception) {
            throw OAuthTokenException("Error getting private key", e)
        }
    }

    fun getCertificate(resource: Resource?): X509Certificate {
        if (resource == null) {
            throw OAuthTokenException("No certificate provided")
        } else if (!resource.isReadable) {
            throw OAuthTokenException("Certificate ${resource.description} is not readable")
        }

        try {
            logger.info { "Reading certificate: ${resource.description}" }
            val certificateContent = readPEMFile(resource)
            val inputStream = Base64.getDecoder().decode(certificateContent).inputStream()
            return CertificateFactory.getInstance("X.509").generateCertificate(inputStream) as X509Certificate
        } catch (e: Exception) {
            throw OAuthTokenException("Error getting certificate", e)
        }
    }

    /**
     * Validates if the client assertion resource is readable and not empty on init. The function then returns a
     * callable of the file contents because the client assertion might refresh while the application is running.
     */
    fun getClientAssertionCallable(resource: Resource): Callable<String> {
        if (!resource.isReadable) {
            throw OAuthTokenException("Client assertion ${resource.description} is not readable")
        }
        if (resource.contentLength() == 0L) {
            throw OAuthTokenException("Client assertion ${resource.description} is empty")
        }

        return { resource.contentAsByteArray.decodeToString().trim() }
    }

    private fun readPEMFile(resource: Resource): String =
        resource.contentAsByteArray.decodeToString().filterNot { it.isWhitespace() }.replace(PEM_REMOVAL_PATTERN, "")
}

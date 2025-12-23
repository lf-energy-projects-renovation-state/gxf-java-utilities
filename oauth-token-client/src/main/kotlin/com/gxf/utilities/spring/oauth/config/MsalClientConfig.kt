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
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource

@Configuration
@Conditional(OAuthMsalEnabledCondition::class)
class MsalClientConfig {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MsalClientConfig::class.java)
        private val PEM_REMOVAL_PATTERN = Regex("-----[A-Z ]*-----")
    }

    @Bean
    fun clientCredentialParameters(properties: OAuthClientProperties): ClientCredentialParameters {
        return ClientCredentialParameters.builder(setOf(properties.scope)).build()
    }

    @Bean
    fun confidentialClientApplication(properties: OAuthClientProperties): ConfidentialClientApplication {
        val credential: IClientCredential =
            ClientCredentialFactory.createFromCertificate(
                getPrivateKey(properties.privateKey),
                getCertificate(properties.certificate),
            )
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
            LOGGER.info("Reading private key: ${resource.description}")
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
            LOGGER.info("Reading certificate: ${resource.description}")
            val certificateContent = readPEMFile(resource)
            val inputStream = Base64.getDecoder().decode(certificateContent).inputStream()
            return CertificateFactory.getInstance("X.509").generateCertificate(inputStream) as X509Certificate
        } catch (e: Exception) {
            throw OAuthTokenException("Error getting certificate", e)
        }
    }

    private fun readPEMFile(resource: Resource): String =
        resource.contentAsByteArray.decodeToString().filterNot { it.isWhitespace() }.replace(PEM_REMOVAL_PATTERN, "")
}

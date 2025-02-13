// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.spring.oauth.config

import com.gxf.utilities.spring.oauth.config.condition.OAuthEnabledCondition
import com.gxf.utilities.spring.oauth.exceptions.OAuthTokenException
import com.microsoft.aad.msal4j.ClientCredentialFactory
import com.microsoft.aad.msal4j.ClientCredentialParameters
import com.microsoft.aad.msal4j.ConfidentialClientApplication
import com.microsoft.aad.msal4j.IClientCredential
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.stream.Collectors
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource

@Configuration
@Conditional(OAuthEnabledCondition::class)
class OAuthClientConfig {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(OAuthClientConfig::class.java)
    }

    @Bean
    fun clientCredentialParameters(clientData: OAuthClientProperties): ClientCredentialParameters {
        return ClientCredentialParameters.builder(setOf(clientData.scope)).build()
    }

    @Bean
    fun confidentialClientApplication(clientData: OAuthClientProperties): ConfidentialClientApplication {
        val credential: IClientCredential =
            ClientCredentialFactory.createFromCertificate(
                getPrivateKey(Objects.requireNonNull(clientData.privateKey)),
                getCertificate(Objects.requireNonNull(clientData.certificate)),
            )
        return try {
            ConfidentialClientApplication.builder(clientData.clientId, credential)
                .authority(clientData.tokenEndpoint)
                .build()
        } catch (e: Exception) {
            throw OAuthTokenException("Error creating client credentials", e)
        }
    }

    /** Reads a private key file and puts */
    fun getPrivateKey(resource: Resource): PrivateKey {
        try {
            LOGGER.info("Reading private key from: ${resource.uri}")
            Files.lines(resource.file.toPath()).use { lines ->
                val privateKeyContent =
                    lines
                        .filter { line: String -> !line.matches("-----[A-Z ]*-----".toRegex()) }
                        .collect(Collectors.joining())
                val kf = KeyFactory.getInstance("RSA")
                val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent))
                return kf.generatePrivate(keySpecPKCS8)
            }
        } catch (e: Exception) {
            throw OAuthTokenException("Error getting private key", e)
        }
    }

    fun getCertificate(resource: Resource): X509Certificate {
        try {
            LOGGER.info("Reading certificate from: ${resource.uri}")
            Files.lines(resource.file.toPath()).use { lines ->
                val certificateContent =
                    lines
                        .filter { line: String -> !line.matches("-----[A-Z ]*-----".toRegex()) }
                        .collect(Collectors.joining())
                val inputStream = ByteArrayInputStream(Base64.getDecoder().decode(certificateContent))
                return CertificateFactory.getInstance("X.509").generateCertificate(inputStream) as X509Certificate
            }
        } catch (e: Exception) {
            throw OAuthTokenException("Error getting certificate", e)
        }
    }
}

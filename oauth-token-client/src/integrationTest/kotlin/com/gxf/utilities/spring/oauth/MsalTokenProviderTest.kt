// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.spring.oauth

import com.gxf.utilities.spring.oauth.providers.TokenProvider
import com.microsoft.aad.msal4j.ClientCredentialParameters
import com.microsoft.aad.msal4j.ConfidentialClientApplication
import com.microsoft.aad.msal4j.IAccount
import com.microsoft.aad.msal4j.IAuthenticationResult
import com.microsoft.aad.msal4j.ITenantProfile
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.util.Date
import java.util.concurrent.CompletableFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

class MsalTokenProviderTest {

    class TestAuthenticationResult(val accessToken: String) : IAuthenticationResult {
        override fun accessToken(): String = accessToken

        override fun idToken(): String? = null

        override fun account(): IAccount? = null

        override fun tenantProfile(): ITenantProfile? = null

        override fun environment(): String? = null

        override fun scopes(): String? = null

        override fun expiresOnDate(): Date? = null
    }

    @Nested
    @SpringJUnitConfig(OAuthTokenClientContext::class)
    @TestPropertySource("classpath:oauth-msal-keypair.properties")
    inner class TestMsalKeyPairConfiguration {
        @MockkBean lateinit var confidentialClientApplication: ConfidentialClientApplication
        @Autowired lateinit var tokenProvider: TokenProvider

        @Test
        fun `should retrieve token from msal library`() {
            val testToken = "test-token-value"
            every { confidentialClientApplication.acquireToken(any<ClientCredentialParameters>()) } returns
                CompletableFuture.supplyAsync { TestAuthenticationResult(testToken) }

            assertThat(tokenProvider.getAccessToken()).contains(testToken)
        }
    }

    @Nested
    @SpringJUnitConfig(OAuthTokenClientContext::class)
    @TestPropertySource("classpath:oauth-msal-client-assertion.properties")
    inner class TestMsalClientAssertionConfiguration {
        @MockkBean lateinit var confidentialClientApplication: ConfidentialClientApplication
        @Autowired lateinit var tokenProvider: TokenProvider

        @Test
        fun `should retrieve token from msal library`() {
            val testToken = "test-token-value"
            every { confidentialClientApplication.acquireToken(any<ClientCredentialParameters>()) } returns
                CompletableFuture.supplyAsync { TestAuthenticationResult(testToken) }

            assertThat(tokenProvider.getAccessToken()).contains(testToken)
        }
    }

    @Nested
    @SpringJUnitConfig(OAuthTokenClientContext::class)
    @TestPropertySource("classpath:oauth-msal-keypair.properties")
    inner class TestMsalKeyPairTokenProvider {

        @Autowired lateinit var tokenProvider: TokenProvider

        @Test
        fun `msal token provider should bootstrap if confidential client application is not mocked`() {
            // The msal4j library is very hard to mock properly
            assertThat(tokenProvider).isNotNull()
        }
    }

    @Nested
    @SpringJUnitConfig(OAuthTokenClientContext::class)
    @TestPropertySource("classpath:oauth-msal-client-assertion.properties")
    inner class TestMsalClientAssertionTokenProvider {

        @Autowired lateinit var tokenProvider: TokenProvider

        @Test
        fun `msal token provider should bootstrap if confidential client application is not mocked`() {
            // The msal4j library is very hard to mock properly
            assertThat(tokenProvider).isNotNull()
        }
    }
}

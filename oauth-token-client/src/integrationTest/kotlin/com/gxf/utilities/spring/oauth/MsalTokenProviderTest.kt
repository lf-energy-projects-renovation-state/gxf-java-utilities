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
import java.util.Date
import java.util.concurrent.CompletableFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

@SpringJUnitConfig(OAuthTokenClientContext::class)
@TestPropertySource("classpath:oauth-msal.properties")
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

    @MockitoBean lateinit var confidentialClientApplication: ConfidentialClientApplication

    @Autowired lateinit var tokenProvider: TokenProvider

    @Test
    fun test() {
        val testToken = "test-token-value"
        `when`(confidentialClientApplication.acquireToken(any<ClientCredentialParameters>()))
            .thenReturn(CompletableFuture.supplyAsync { TestAuthenticationResult(testToken) })

        assertThat(tokenProvider.getAccessToken()).contains(testToken)
    }
}

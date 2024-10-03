// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.spring.oauth.providers

import com.gxf.utilities.spring.oauth.config.condition.OAuthEnabledCondition
import com.microsoft.aad.msal4j.ClientCredentialParameters
import com.microsoft.aad.msal4j.ConfidentialClientApplication
import com.microsoft.aad.msal4j.IAuthenticationResult
import java.util.Optional
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
@Conditional(OAuthEnabledCondition::class)
class OAuthTokenProvider(
    private val confidentialClientApplication: ConfidentialClientApplication,
    private val parameters: ClientCredentialParameters,
) : TokenProvider {

    /**
     * Retrieve an oauth token from the oauth provider. Tokens are cached by the msal4j Library so no caching needed
     * here.
     */
    override fun getAccessToken(): Optional<String> = Optional.of(getOAuthToken().accessToken())

    /**
     * Retrieve an oauth object from the oauth provider. Tokens are cached by the msal4j Library so no caching needed
     * here.
     */
    fun getOAuthToken(): IAuthenticationResult = confidentialClientApplication.acquireToken(parameters).join()
}

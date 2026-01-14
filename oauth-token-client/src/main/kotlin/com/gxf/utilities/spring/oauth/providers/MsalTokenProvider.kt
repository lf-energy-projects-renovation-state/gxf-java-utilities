// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.spring.oauth.providers

import com.gxf.utilities.spring.oauth.config.condition.OAuthMsalEnabledCondition
import com.microsoft.aad.msal4j.ClientCredentialParameters
import com.microsoft.aad.msal4j.ConfidentialClientApplication
import com.microsoft.aad.msal4j.IAuthenticationResult
import java.util.Optional
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Component

@Component
@Conditional(OAuthMsalEnabledCondition::class)
internal class MsalTokenProvider(
    private val confidentialClientApplication: ConfidentialClientApplication,
    private val parameters: ClientCredentialParameters,
) : TokenProvider {

    /**
     * Retrieve an oauth token from the oauth provider. Tokens are cached by the msal4j library, so no caching needed
     * here.
     */
    override fun getAccessToken(): Optional<String> = Optional.of(getOAuthToken().accessToken())

    private fun getOAuthToken(): IAuthenticationResult = confidentialClientApplication.acquireToken(parameters).join()
}

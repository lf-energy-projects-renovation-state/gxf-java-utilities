/*
SPDX-FileCopyrightText: Contributors to the GXF project

SPDX-License-Identifier: Apache-2.0
*/
package com.gxf.utilities.spring.oauth.services

import com.gxf.utilities.spring.oauth.config.OauthClientCondition
import com.microsoft.aad.msal4j.ClientCredentialParameters
import com.microsoft.aad.msal4j.ConfidentialClientApplication
import com.microsoft.aad.msal4j.IAuthenticationResult
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

@Service
@Conditional(OauthClientCondition::class)
class OAuthTokenProvider(
    private val confidentialClientApplication: ConfidentialClientApplication,
    private val parameters: ClientCredentialParameters
) {

    /**
     * Retrieve an oauth token from the oauth provider.
     * Tokens are cached by the msal4j Library so no caching needed here.
     */
    fun getAccessToken(): String =
        getOAuthToken().accessToken()

    /**
     * Retrieve an oauth object from the oauth provider.
     * Tokens are cached by the msal4j Library so no caching needed here.
     */
    fun getOAuthToken(): IAuthenticationResult =
        confidentialClientApplication.acquireToken(parameters).join()
}

// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.spring.oauth.providers

import com.gxf.utilities.spring.oauth.config.OAuthClientProperties
import com.gxf.utilities.spring.oauth.config.condition.OAuthTokenResourceEnabledCondition
import com.gxf.utilities.spring.oauth.exceptions.OAuthTokenException
import java.nio.charset.Charset
import java.util.Optional
import org.springframework.context.annotation.Conditional
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service

@Service
@Conditional(OAuthTokenResourceEnabledCondition::class)
internal final class FileTokenProvider(clientProperties: OAuthClientProperties) : TokenProvider {

    private val tokenResource: Resource

    init {
        if (clientProperties.tokenLocation == null) {
            throw OAuthTokenException("The token location property is required")
        } else if (!clientProperties.tokenLocation.isReadable) {
            throw OAuthTokenException("The token location '${clientProperties.tokenLocation}` is not readable")
        }
        tokenResource = clientProperties.tokenLocation
    }

    /** Read the resource file everytime since it may be updated while the application is running */
    override fun getAccessToken(): Optional<String> =
        Optional.of(tokenResource.getContentAsString(Charset.defaultCharset()).trim())
}

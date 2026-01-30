// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.spring.oauth.providers

import com.gxf.utilities.spring.oauth.config.OAuthClientProperties
import com.gxf.utilities.spring.oauth.config.condition.OAuthTokenFileEnabledCondition
import com.gxf.utilities.spring.oauth.exceptions.OAuthTokenException
import java.nio.charset.Charset
import java.util.Optional
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Conditional
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
@Conditional(OAuthTokenFileEnabledCondition::class)
internal final class FileTokenProvider(clientProperties: OAuthClientProperties) : TokenProvider {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(FileTokenProvider::class.java)
    }

    private val tokenResource: Resource

    init {
        if (clientProperties.tokenLocation?.isReadable != true) {
            throw OAuthTokenException("The token location '${clientProperties.tokenLocation}` is not readable")
        }
        tokenResource = clientProperties.tokenLocation
        LOGGER.info("Configured File Token Provider with token location: ${clientProperties.tokenLocation.description}")
    }

    /** Read the resource file everytime since it may be updated while the application is running */
    override fun getAccessToken(): Optional<String> =
        Optional.of(tokenResource.getContentAsString(Charset.defaultCharset()).trim())
}

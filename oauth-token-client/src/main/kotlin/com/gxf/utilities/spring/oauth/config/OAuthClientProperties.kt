// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.spring.oauth.config

import com.gxf.utilities.spring.oauth.config.condition.OAuthMsalEnabledCondition
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Conditional
import org.springframework.core.io.Resource

@Conditional(OAuthMsalEnabledCondition::class)
@ConfigurationProperties(prefix = "oauth.client")
class OAuthClientProperties(
    val tokenLocation: Resource?,
    val clientId: String?,
    val scope: String?,
    val tokenEndpoint: String?,
    val certificate: Resource?,
    val privateKey: Resource?,
)

// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.spring.oauth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.Resource

@ConfigurationProperties(prefix = "oauth.client")
class OAuthClientProperties(
    val tokenLocation: Resource?,
    val clientId: String?,
    val scope: String?,
    val tokenEndpoint: String?,
    val certificate: Resource?,
    val privateKey: Resource?,
)

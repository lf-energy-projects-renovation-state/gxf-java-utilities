/*
SPDX-FileCopyrightText: Contributors to the GXF project

SPDX-License-Identifier: Apache-2.0
*/
package com.gxf.utilities.spring.oauth.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource

@Configuration
@Conditional(OauthClientCondition::class)
class OAuthClientProperties(
    @Value("\${oauth.client.client-id}")
    val clientId: String,
    @Value("\${oauth.client.scope}")
    val scope: String,
    @Value("\${oauth.client.token-endpoint}")
    val tokenEndpoint: String,
    @Value("\${oauth.client.private-key}")
    val privateKey: Resource,
    @Value("\${oauth.client.certificate}")
    val certificate: Resource
)

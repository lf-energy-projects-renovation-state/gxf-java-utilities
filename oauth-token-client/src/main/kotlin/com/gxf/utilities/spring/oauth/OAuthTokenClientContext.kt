/*
SPDX-FileCopyrightText: Contributors to the GXF project

SPDX-License-Identifier: Apache-2.0
*/
package com.gxf.utilities.spring.oauth

import com.gxf.utilities.spring.oauth.config.OAuthClientConfig
import com.gxf.utilities.spring.oauth.config.OAuthClientProperties
import com.gxf.utilities.spring.oauth.services.OAuthTokenProvider
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(OAuthClientConfig::class, OAuthTokenProvider::class, OAuthClientProperties::class)
class OAuthTokenClientContext

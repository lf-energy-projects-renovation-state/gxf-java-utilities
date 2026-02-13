// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.spring.oauth

import com.gxf.utilities.spring.oauth.config.MsalClientConfig
import com.gxf.utilities.spring.oauth.config.OAuthClientProperties
import com.gxf.utilities.spring.oauth.providers.MsalTokenProvider
import com.gxf.utilities.spring.oauth.providers.NoTokenProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component

@Component
@EnableConfigurationProperties(OAuthClientProperties::class)
@Import(MsalClientConfig::class, MsalTokenProvider::class, NoTokenProvider::class)
class OAuthTokenClientContext

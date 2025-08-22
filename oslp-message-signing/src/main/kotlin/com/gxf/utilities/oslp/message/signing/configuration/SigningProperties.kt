// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.oslp.message.signing.configuration

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties

@ConditionalOnMissingBean(SigningProperties::class)
@ConfigurationProperties(prefix = "signing")
class SigningProperties(val securityProvider: String, val securityAlgorithm: String)

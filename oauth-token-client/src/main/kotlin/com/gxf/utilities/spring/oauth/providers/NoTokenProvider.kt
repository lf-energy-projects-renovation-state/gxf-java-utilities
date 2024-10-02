// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.spring.oauth.providers

import com.gxf.utilities.spring.oauth.config.condition.OAuthDisabledCondition
import java.util.*
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

@Service
@Conditional(OAuthDisabledCondition::class)
class NoTokenProvider : TokenProvider {
    /** Returns an empty optional indicating that no oauth provider is configured. */
    override fun getAccessToken(): Optional<String> = Optional.empty<String>()
}

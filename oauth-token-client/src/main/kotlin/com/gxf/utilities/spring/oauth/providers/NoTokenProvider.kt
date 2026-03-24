// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.spring.oauth.providers

import org.springframework.context.annotation.Fallback
import org.springframework.stereotype.Component
import java.util.*

@Component
@Fallback
internal class NoTokenProvider : TokenProvider {
    /** Returns an empty optional indicating that no oauth provider is configured. */
    override fun getAccessToken(): Optional<String> = Optional.empty()
}

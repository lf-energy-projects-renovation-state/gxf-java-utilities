// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.spring.oauth

import com.gxf.utilities.spring.oauth.providers.OAuthTokenProvider
import com.gxf.utilities.spring.oauth.providers.TokenProvider
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

@SpringJUnitConfig(OAuthTokenClientContext::class)
@TestPropertySource("classpath:oauth-enabled.properties")
class OAuthTokenProviderTest {

    @Autowired lateinit var tokenProvider: TokenProvider

    @Test
    fun test() {
        assert(tokenProvider is OAuthTokenProvider)
    }
}

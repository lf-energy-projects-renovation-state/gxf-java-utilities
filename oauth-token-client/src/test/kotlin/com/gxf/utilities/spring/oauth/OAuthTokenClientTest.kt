/*
SPDX-FileCopyrightText: Contributors to the GXF project

SPDX-License-Identifier: Apache-2.0
*/
package com.gxf.utilities.spring.oauth

import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

@SpringJUnitConfig(OAuthTokenClientContext::class)
@TestPropertySource("classpath:oauth-token-client-test.properties")
class OAuthTokenClientTest {

    @Test
    fun shouldConfigure() {

    }
}

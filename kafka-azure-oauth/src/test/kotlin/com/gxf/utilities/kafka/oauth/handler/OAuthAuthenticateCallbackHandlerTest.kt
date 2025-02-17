// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.oauth.handler

import com.gxf.utilities.kafka.oauth.handler.OAuthAuthenticateCallbackHandler.Companion.CLIENT_ID_CONFIG
import com.gxf.utilities.kafka.oauth.handler.OAuthAuthenticateCallbackHandler.Companion.SCOPE_CONFIG
import com.gxf.utilities.kafka.oauth.handler.OAuthAuthenticateCallbackHandler.Companion.TOKEN_ENDPOINT_CONFIG
import com.gxf.utilities.kafka.oauth.handler.OAuthAuthenticateCallbackHandler.Companion.TOKEN_FILE_CONFIG
import java.lang.IllegalArgumentException
import javax.security.auth.login.AppConfigurationEntry
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.REQUIRED
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OAuthAuthenticateCallbackHandlerTest {

    private val clientId = "a561c63a-5faa-48a7-99db-e5a7c0f1c198"
    private val tokenEndpoint = "https://localhost/token"
    private val scopes = "testing-one,testing-two"
    private val tokenFilePath = "src/test/resources/tokenFile"

    @Test
    fun configure() {
        val handler = OAuthAuthenticateCallbackHandler()

        val appConfig =
            AppConfigurationEntry(
                "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule",
                REQUIRED,
                options(),
            )
        handler.configure(emptyMap<String?, Any>(), OAuthBearerLoginModule.OAUTHBEARER_MECHANISM, listOf(appConfig))

        assertEquals(clientId, handler.clientId)
        assertEquals(tokenEndpoint, handler.tokenEndpoint)
        assertEquals(setOf("testing-one", "testing-two"), handler.scopes)
        assertEquals(tokenFilePath, handler.tokenFilePath)
    }

    @Test
    fun `configure without jaas entries`() {
        val handler = OAuthAuthenticateCallbackHandler()

        Assertions.assertThatThrownBy {
                handler.configure(emptyMap<String?, Any>(), OAuthBearerLoginModule.OAUTHBEARER_MECHANISM, emptyList())
            }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Must supply exactly 1 non-null JAAS mechanism configuration (size was 0)")
    }

    @Test
    fun `read token file`() {
        val handler = OAuthAuthenticateCallbackHandler()
        val tokenFileContent = handler.readTokenFile(tokenFilePath)
        assertEquals("test-token", tokenFileContent)
    }

    @Test
    fun `read token file exception`() {
        val handler = OAuthAuthenticateCallbackHandler()

        Assertions.assertThatThrownBy { handler.readTokenFile("non-existent-file") }
            .isInstanceOf(KafkaOAuthException::class.java)
            .hasMessage("Could not read Token file from: non-existent-file")
    }

    private fun options() =
        mapOf(
            CLIENT_ID_CONFIG to clientId,
            TOKEN_ENDPOINT_CONFIG to tokenEndpoint,
            SCOPE_CONFIG to scopes,
            TOKEN_FILE_CONFIG to tokenFilePath,
        )
}

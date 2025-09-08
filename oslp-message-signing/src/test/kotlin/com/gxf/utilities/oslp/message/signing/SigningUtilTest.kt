// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.oslp.message.signing

import com.gxf.utilities.oslp.message.signing.configuration.SigningProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SigningUtilTest {

    val signingProperties: SigningProperties =
        SigningProperties(securityProvider = "SunEC", securityAlgorithm = "SHA256withECDSA")
    val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance("EC").apply { initialize(256) }
    val keyPair1: KeyPair = keyPairGenerator.generateKeyPair()
    val keyPair2: KeyPair = keyPairGenerator.generateKeyPair()

    private val signingUtil: SigningUtil = SigningUtil(signingProperties)

    @Test
    fun `should sign and verify message from different SigningUtils with same keys`() {
        val message = "test-message".toByteArray()
        val signature = signingUtil.createSignature(message, keyPair1.private)
        assertThat(signingUtil.verifySignature(message, signature, keyPair1.public)).isTrue()
    }

    @Test
    fun `should not verify tampered message`() {
        var message = "test-message".toByteArray()
        val signature = signingUtil.createSignature(message, keyPair1.private)
        message = "tampered-message".toByteArray()
        assertThat(signingUtil.verifySignature(message, signature, keyPair1.public)).isFalse()
    }

    @Test
    fun `should not verify tampered keys`() {
        val message = "test-message".toByteArray()
        val signature = signingUtil.createSignature(message, keyPair1.private)
        assertThat(signingUtil.verifySignature(message, signature, keyPair2.public)).isFalse()
    }
}

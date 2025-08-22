// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.oslp.message.signing

import com.gxf.utilities.oslp.message.signing.configuration.SigningProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SigningUtilTest {

    val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance("EC").apply { initialize(256) }
    val keyPair1: KeyPair = keyPairGenerator.generateKeyPair()
    val keyPair2: KeyPair = keyPairGenerator.generateKeyPair()

    class TestKeyProvider(private val privateKey: PrivateKey, private val publicKey: PublicKey) : KeyProvider {
        override fun getPrivateKey() = privateKey

        override fun getPublicKey() = publicKey
    }

    val keyProvider1: TestKeyProvider = TestKeyProvider(privateKey = keyPair1.private, publicKey = keyPair1.public)

    val keyProvider2: TestKeyProvider = TestKeyProvider(privateKey = keyPair2.private, publicKey = keyPair2.public)

    private val signingUtil1: SigningUtil =
        SigningUtil(
            signingConfiguration = SigningProperties(securityProvider = "SunEC", securityAlgorithm = "SHA256withECDSA"),
            keyProvider = keyProvider1,
        )

    private val signingUtil2: SigningUtil =
        SigningUtil(
            signingConfiguration = SigningProperties(securityProvider = "SunEC", securityAlgorithm = "SHA256withECDSA"),
            keyProvider = keyProvider2,
        )

    private val signingUtil3: SigningUtil =
        SigningUtil(
            signingConfiguration = SigningProperties(securityProvider = "SunEC", securityAlgorithm = "SHA256withECDSA"),
            keyProvider = keyProvider1,
        )

    @Test
    fun `should sign and verify message from different SigningUtils with same keys`() {
        val message = "test-message".toByteArray()
        val signature = signingUtil1.createSignature(message)
        assertThat(signingUtil3.verifySignature(message, signature)).isTrue()
    }

    @Test
    fun `should not verify tampered message`() {
        var message = "test-message".toByteArray()
        val signature = signingUtil1.createSignature(message)
        message = "tampered-message".toByteArray()
        assertThat(signingUtil1.verifySignature(message, signature)).isFalse()
    }

    @Test
    fun `should not verify tampered keys`() {
        val message = "test-message".toByteArray()
        val signature = signingUtil1.createSignature(message)
        assertThat(signingUtil2.verifySignature(message, signature)).isFalse()
    }
}

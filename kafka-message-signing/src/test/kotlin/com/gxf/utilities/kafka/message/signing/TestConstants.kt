// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing

import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Random
import org.springframework.core.io.ClassPathResource

object TestConstants {
    val messageSignerProperties =
        MessageSigningProperties(
            signingEnabled = true,
            stripAvroHeader = true,
            signatureAlgorithm = "SHA256withRSA",
            signatureProvider = "SunRsaSign",
            keyAlgorithm = "RSA",
            privateKeyFile = ClassPathResource("/rsa-private.pem"),
            publicKeyFile = ClassPathResource("/rsa-public.pem"),
            previousPublicKeyFile = ClassPathResource("/rsa-public-previous.pem"),
            previousPrivateKeyFile = ClassPathResource("/rsa-private-previous.pem"),
        )

    fun randomSignature(): ByteBuffer {
        val random: Random = SecureRandom()
        val keySize = 2048

        val signature = ByteArray(keySize / 8)
        random.nextBytes(signature)

        return ByteBuffer.wrap(signature)
    }
}

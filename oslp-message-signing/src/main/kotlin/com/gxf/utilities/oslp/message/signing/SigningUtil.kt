// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.oslp.message.signing

import com.gxf.utilities.oslp.message.signing.configuration.SigningProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import java.security.SecureRandom
import java.security.Signature

open class SigningUtil(val signingConfiguration: SigningProperties, val keyProvider: KeyProvider) {
    private val logger = KotlinLogging.logger {}

    fun createSignature(message: ByteArray): ByteArray {
        logger.debug { "Creating signature for message of length: ${message.size}" }
        return Signature.getInstance(signingConfiguration.securityAlgorithm, signingConfiguration.securityProvider)
            .apply {
                initSign(keyProvider.getPrivateKey(), SecureRandom())
                update(message)
            }
            .sign()
    }

    fun verifySignature(message: ByteArray, securityKey: ByteArray): Boolean {
        logger.debug { "Verifying signature for message of length: ${message.size}" }
        val builder =
            Signature.getInstance(signingConfiguration.securityAlgorithm, signingConfiguration.securityProvider).apply {
                initVerify(keyProvider.getPublicKey())
                update(message)
            }

        // Truncation needed for some signature types, including the used SHA256withECDSA
        val len = securityKey[1] + 2 and 0xff
        val truncatedKey = securityKey.copyOf(len)

        return builder.verify(truncatedKey)
    }
}

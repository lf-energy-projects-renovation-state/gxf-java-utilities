// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.oslp.message.signing

import com.gxf.utilities.oslp.message.signing.AlgorithmConstants.SHA256_WITH_ECDSA
import com.gxf.utilities.oslp.message.signing.configuration.SigningProperties
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature

open class SigningUtil(val signingConfiguration: SigningProperties) {

    fun createSignature(message: ByteArray, privateKey: PrivateKey): ByteArray {
        return Signature.getInstance(signingConfiguration.securityAlgorithm, signingConfiguration.securityProvider)
            .apply {
                initSign(privateKey, SecureRandom())
                update(message)
            }
            .sign()
    }

    fun verifySignature(message: ByteArray, securityKey: ByteArray, publicKey: PublicKey): Boolean {
        val builder =
            Signature.getInstance(signingConfiguration.securityAlgorithm, signingConfiguration.securityProvider).apply {
                initVerify(publicKey)
                update(message)
            }

        if (signingConfiguration.securityAlgorithm in algorithmsToTruncate) {
            val len = securityKey[1] + 2 and 0xff
            val truncatedKey = securityKey.copyOf(len)
            return builder.verify(truncatedKey)
        }

        return builder.verify(securityKey)
    }

    private companion object {
        private val algorithmsToTruncate = listOf(SHA256_WITH_ECDSA)
    }
}

// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing

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
        )
}

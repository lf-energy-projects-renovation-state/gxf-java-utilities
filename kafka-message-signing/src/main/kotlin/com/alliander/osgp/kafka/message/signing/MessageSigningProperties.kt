// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package com.alliander.osgp.kafka.message.signing

import java.security.PrivateKey
import java.security.PublicKey

open class MessageSigningProperties {
    var signingEnabled: Boolean = false
    var stripAvroHeader: Boolean = false

    var signatureAlgorithm: String = MessageSigner.DEFAULT_SIGNATURE_ALGORITHM
    var signatureProvider: String? = MessageSigner.DEFAULT_SIGNATURE_PROVIDER
    var signatureKeyAlgorithm: String = MessageSigner.DEFAULT_SIGNATURE_KEY_ALGORITHM
    var signatureKeySize: Int = MessageSigner.DEFAULT_SIGNATURE_KEY_SIZE

    var signingKey: PrivateKey? = null
    var verificationKey: PublicKey? = null
}

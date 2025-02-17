// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.Resource

@ConfigurationProperties(prefix = "message-signing")
// Only instantiate when no other bean has been configured
@ConditionalOnMissingBean(MessageSigningProperties::class)
open class MessageSigningProperties(
    /** Enable or disable signing */
    var signingEnabled: Boolean = false,
    /** Strip the Avro header containing the schema fingerprint */
    var stripAvroHeader: Boolean = false,

    /** Signature algorithm */
    var signatureAlgorithm: String = MessageSigner.DEFAULT_SIGNATURE_ALGORITHM,
    /** Signature algorithm provider */
    var signatureProvider: String? = MessageSigner.DEFAULT_SIGNATURE_PROVIDER,
    /** Public key algorithm */
    var keyAlgorithm: String = MessageSigner.DEFAULT_KEY_ALGORITHM,
    /** PEM file containing the private key */
    var privateKeyFile: Resource? = null,
    /** PEM file containing the public key */
    var publicKeyFile: Resource? = null,
)

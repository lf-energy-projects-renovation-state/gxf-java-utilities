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
open class MessageSigningProperties {
    var signingEnabled: Boolean = false
    var stripAvroHeader: Boolean = false

    var algorithm: String = MessageSigner.DEFAULT_SIGNATURE_ALGORITHM
    var provider: String? = MessageSigner.DEFAULT_SIGNATURE_PROVIDER
    var keyAlgorithm: String = MessageSigner.DEFAULT_SIGNATURE_KEY_ALGORITHM
    var keySize: Int = MessageSigner.DEFAULT_SIGNATURE_KEY_SIZE
    var privateKeyFile: Resource? = null
    var publicKeyFile: Resource? = null
}

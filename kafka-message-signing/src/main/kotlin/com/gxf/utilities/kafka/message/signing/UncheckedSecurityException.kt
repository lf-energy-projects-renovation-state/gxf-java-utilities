// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing

import java.io.Serial
import java.security.GeneralSecurityException

class UncheckedSecurityException @JvmOverloads constructor(message: String? = null, cause: GeneralSecurityException) :
    RuntimeException(message, cause) {

    @Serial private val serialVersionUID = 5152038114753546167L

    override val cause: GeneralSecurityException
        get() = super.cause as GeneralSecurityException
}

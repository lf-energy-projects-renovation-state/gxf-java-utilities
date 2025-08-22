// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.oslp.message.signing

import java.security.PrivateKey
import java.security.PublicKey
import org.springframework.stereotype.Component

@Component
interface KeyProvider {

    fun getPublicKey(): PublicKey

    fun getPrivateKey(): PrivateKey
}

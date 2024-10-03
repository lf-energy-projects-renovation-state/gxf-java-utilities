// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.spring.oauth.providers

import java.util.*

fun interface TokenProvider {
    /**
     * Retrieve an access token for instance from an oauth provider. Tokens are cached internally, there is no need to
     * cache them outside of this library.
     */
    fun getAccessToken(): Optional<String>
}

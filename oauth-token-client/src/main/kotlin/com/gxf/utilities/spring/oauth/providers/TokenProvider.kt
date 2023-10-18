package com.gxf.utilities.spring.oauth.providers

import java.util.*

interface TokenProvider {
    /**
     * Retrieve an access token for instance from an oauth provider.
     * Tokens are cached internally, there is no need to cache them outside of this library.
     */
    fun getAccessToken(): Optional<String>
}

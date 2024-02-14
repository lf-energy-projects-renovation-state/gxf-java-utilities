// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package com.gxf.utilities.kafka.message.signing

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding
import org.springframework.boot.ssl.pem.PemContent
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.charset.StandardCharsets
import java.security.PrivateKey

/**
 * Converts a String (filename) to a PrivateKey object
 */
@Component
@ConfigurationPropertiesBinding
class PrivateKeyConverter : Converter<String, PrivateKey> {
    /**
     * Converts the given source to a PrivateKey object
     *
     * @param source The filename, the resource location to resolve: either a "classpath:" pseudo URL, a "file:" URL, or a plain file path
     */
    override fun convert(source: String): PrivateKey? {
        try {
            val content = ResourceUtils.getFile(source).readText(StandardCharsets.ISO_8859_1)
            return PemContent.of(content).privateKey
        } catch (e: IOException) {
            throw UncheckedIOException("Unable to read $source as ISO-LATIN-1 PEM text", e)
        }
    }
}

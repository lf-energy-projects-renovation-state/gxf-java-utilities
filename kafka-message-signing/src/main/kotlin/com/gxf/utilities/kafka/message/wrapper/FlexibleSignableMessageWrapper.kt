// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.wrapper

import java.io.IOException
import java.nio.ByteBuffer

/**
 * Wrapper for signable messages. Because these messages are generated from Avro schemas, they can't be changed. This
 * wrapper unifies them for the MessageSigner.
 */
class FlexibleSignableMessageWrapper<T>(
    val message: T,
    private val messageGetter: (T) -> ByteBuffer,
    private val signatureGetter: (T) -> ByteBuffer?,
    private val signatureSetter: (T, ByteBuffer?) -> Unit,
) {

    /** @return ByteBuffer of the whole message */
    @Throws(IOException::class) internal fun toByteBuffer(): ByteBuffer = messageGetter(message)

    /** @return ByteBuffer of the signature in the message */
    internal fun getSignature(): ByteBuffer? = signatureGetter(message)

    /** @param signature The signature in ByteBuffer form to be set on the message */
    internal fun setSignature(signature: ByteBuffer) {
        signatureSetter(message, signature)
    }

    internal fun clearSignature() {
        signatureSetter(message, null)
    }
}

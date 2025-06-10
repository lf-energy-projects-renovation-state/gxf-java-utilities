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
@Deprecated("Use SignableMessage instead. This class will be removed in a future release.")
abstract class SignableMessageWrapper<T>(val message: T) {

    /** @return ByteBuffer of the whole message */
    @Throws(IOException::class) abstract fun toByteBuffer(): ByteBuffer

    /** @return ByteBuffer of the signature in the message */
    abstract fun getSignature(): ByteBuffer?

    /** @param signature The signature in ByteBuffer form to be set on the message */
    abstract fun setSignature(signature: ByteBuffer?)

    // For backwards compatibility
    internal fun toFlexibleWrapper() =
        FlexibleSignableMessageWrapper(
            message,
            { _ -> toByteBuffer() },
            { _ -> getSignature() },
            { _, signature -> setSignature(signature) },
        )
}

// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.wrapper

import java.io.IOException

/**
 * Wrapper for signable messages. Because these messages are generated from Avro schemas, they can't be changed. This
 * wrapper unifies them for the MessageSigner.
 */
abstract class SignableMessageWrapper<T>(val message: T) {

    /** @return ByteArray of the whole message */
    @Throws(IOException::class) abstract fun toByteArray(): ByteArray

    /** @return ByteArray of the signature in the message */
    abstract fun getSignature(): ByteArray?

    /** @param signature The signature in ByteArray form to be set on the message */
    abstract fun setSignature(signature: ByteArray?)
}

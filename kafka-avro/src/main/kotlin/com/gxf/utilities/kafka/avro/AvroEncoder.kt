// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.avro

import java.io.IOException
import java.io.OutputStream
import kotlin.reflect.KClass
import org.apache.avro.message.BinaryMessageEncoder
import org.apache.avro.specific.SpecificData
import org.apache.avro.specific.SpecificRecordBase
import org.slf4j.LoggerFactory

object AvroEncoder {
    val encoders: HashMap<KClass<out SpecificRecordBase>, BinaryMessageEncoder<SpecificRecordBase>> = HashMap()

    private val logger = LoggerFactory.getLogger(AvroEncoder::class.java)

    @Throws(IOException::class)
    fun encode(message: SpecificRecordBase): ByteArray {
        val encoder = getEncoder(message)
        val byteBuffer = encoder.encode(message)
        val bytes = ByteArray(byteBuffer.remaining())
        byteBuffer[bytes]
        return bytes
    }

    @Throws(IOException::class)
    fun encode(message: SpecificRecordBase, stream: OutputStream) {
        val encoder = getEncoder(message)
        encoder.encode(message, stream)
    }

    private fun getEncoder(message: SpecificRecordBase): BinaryMessageEncoder<SpecificRecordBase> {
        val existingEncoder = encoders[message::class]

        if (existingEncoder != null) {
            return existingEncoder
        }

        logger.info("New encoder created for Avro schema {}", message::class)
        val newEncoder = BinaryMessageEncoder<SpecificRecordBase>(SpecificData(), message.schema)
        encoders[message::class] = newEncoder
        return newEncoder
    }
}

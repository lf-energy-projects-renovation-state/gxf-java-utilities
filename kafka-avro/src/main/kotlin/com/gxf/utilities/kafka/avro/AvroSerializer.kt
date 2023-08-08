/*
SPDX-FileCopyrightText: Contributors to the GXF project

SPDX-License-Identifier: Apache-2.0
*/
package com.gxf.utilities.kafka.avro

import org.apache.avro.message.BinaryMessageEncoder
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream

class AvroSerializer<T : SpecificRecord>(private val encoder: BinaryMessageEncoder<T>) : Serializer<T> {
    companion object {
        private val logger = LoggerFactory.getLogger(AvroSerializer::class.java)
    }

    /**
     * Serializes a Byte Array to an Avro specific record
     */
    override fun serialize(topic: String?, data: T): ByteArray {
        try {
            logger.trace("Serializing for {}", topic)
            val outputStream = ByteArrayOutputStream()
            encoder.encode(data, outputStream)
            return outputStream.toByteArray()
        } catch (ex: Exception) {
            throw SerializationException("Error serializing Avro message for topic: ${topic}", ex)
        }
    }
}


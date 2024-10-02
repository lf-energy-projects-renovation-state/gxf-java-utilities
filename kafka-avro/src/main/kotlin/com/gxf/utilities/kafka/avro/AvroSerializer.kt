// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.avro

import java.io.ByteArrayOutputStream
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory

class AvroSerializer : Serializer<SpecificRecordBase> {
    companion object {
        private val logger = LoggerFactory.getLogger(AvroSerializer::class.java)
    }

    /** Serializes a Byte Array to an Avro specific record */
    override fun serialize(topic: String?, data: SpecificRecordBase): ByteArray {
        try {
            logger.trace("Serializing for {}", topic)
            val outputStream = ByteArrayOutputStream()
            AvroEncoder.encode(data, outputStream)
            return outputStream.toByteArray()
        } catch (ex: Exception) {
            throw SerializationException("Error serializing Avro message for topic: ${topic}", ex)
        }
    }
}

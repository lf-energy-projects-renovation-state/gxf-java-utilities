// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.avro

import org.apache.avro.Schema
import org.apache.avro.message.BinaryMessageDecoder
import org.apache.avro.specific.SpecificData
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.LoggerFactory

class AvroDeserializer(deserializerSchemas: List<Schema>) : Deserializer<SpecificRecordBase> {
    companion object {
        private val logger = LoggerFactory.getLogger(AvroDeserializer::class.java)
    }

    private val decoder = BinaryMessageDecoder<SpecificRecordBase>(SpecificData(), null)

    init {
        // Add all schemas to the decoder
        deserializerSchemas.forEach { decoder.addSchema(it) }
    }

    /** Deserializes a Byte Array to an Avro SpecificRecordBase */
    override fun deserialize(topic: String, payload: ByteArray): SpecificRecordBase {
        try {
            logger.trace("Deserializing for {}", topic)
            return decoder.decode(payload)
        } catch (ex: Exception) {
            throw SerializationException("Error deserializing Avro message for topic: ${topic}", ex)
        }
    }
}

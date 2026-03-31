// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.avro

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Serializer
import java.io.ByteArrayOutputStream

class AvroSerializer : Serializer<SpecificRecordBase> {
    private val logger = KotlinLogging.logger {}

    /** Serializes an Avro specific record to a ByteArray, returns null if data is null (Kafka Tombstone) */
    override fun serialize(topic: String?, data: SpecificRecordBase?): ByteArray? {
        try {
            return if (data == null) {
                null
            } else {
                logger.trace { "Serializing for $topic" }
                val outputStream = ByteArrayOutputStream()
                AvroEncoder.encode(data, outputStream)
                outputStream.toByteArray()
            }
        } catch (ex: Exception) {
            throw SerializationException("Error serializing Avro message for topic: $topic", ex)
        }
    }
}

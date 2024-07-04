/*
SPDX-FileCopyrightText: Contributors to the GXF project

SPDX-License-Identifier: Apache-2.0
*/
package com.gxf.utilities.kafka.avro

import org.apache.avro.message.BinaryMessageEncoder
import org.apache.avro.specific.SpecificData
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import kotlin.reflect.KClass

class AvroSerializer : Serializer<SpecificRecordBase> {
    val encoders: HashMap<KClass<out SpecificRecordBase>, BinaryMessageEncoder<SpecificRecordBase>> = HashMap()

    companion object {
        private val logger = LoggerFactory.getLogger(AvroSerializer::class.java)
    }

    /**
     * Serializes a Byte Array to an Avro specific record
     */
    override fun serialize(topic: String?, data: SpecificRecordBase): ByteArray {
        try {
            logger.trace("Serializing for {}", topic)
            val outputStream = ByteArrayOutputStream()
            val encoder = getEncoder(data)
            encoder.encode(data, outputStream)
            return outputStream.toByteArray()
        } catch (ex: Exception) {
            throw SerializationException("Error serializing Avro message for topic: ${topic}", ex)
        }
    }

    private fun getEncoder(message: SpecificRecordBase): BinaryMessageEncoder<SpecificRecordBase> {
        val existingEncoder = encoders[message::class]

        if(existingEncoder != null) {
            return existingEncoder
        }

        logger.info("New encoder created for Avro schema {}", message::class)
        val newEncoder = BinaryMessageEncoder<SpecificRecordBase>(SpecificData(), message.schema)
        encoders[message::class] = newEncoder
        return newEncoder
    }
}


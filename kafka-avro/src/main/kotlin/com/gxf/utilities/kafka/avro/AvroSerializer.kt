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

class AvroSerializer<T : SpecificRecordBase> : Serializer<T> {
    private val encoders: HashMap<KClass<out T>, BinaryMessageEncoder<T>> = HashMap()

    companion object {
        private val logger = LoggerFactory.getLogger(AvroSerializer::class.java)
    }

    private fun getEncoder(message: T): BinaryMessageEncoder<T> {
        val existingEncoder = encoders[message::class]

        if(existingEncoder != null) {
            return existingEncoder
        }

        val newEncoder = BinaryMessageEncoder<T>(SpecificData(), message.schema)
        encoders[message::class] = newEncoder
        return newEncoder
    }

    /**
     * Serializes a Byte Array to an Avro specific record
     */
    override fun serialize(topic: String?, data: T): ByteArray {
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
}


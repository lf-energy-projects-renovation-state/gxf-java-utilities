package com.gxf.utilities.kafka.avro

import org.apache.avro.message.BinaryMessageEncoder
import org.apache.avro.specific.SpecificData
import org.apache.avro.specific.SpecificRecordBase
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import kotlin.reflect.KClass

object AvroEncoder {
    val encoders: HashMap<KClass<out SpecificRecordBase>, BinaryMessageEncoder<SpecificRecordBase>> = HashMap()

    private val logger = LoggerFactory.getLogger(AvroEncoder::class.java)

    @Throws(IOException::class)
    fun encode(message: SpecificRecordBase): ByteBuffer {
        val encoder = getEncoder(message)
        return encoder.encode(message)
    }

    @Throws(IOException::class)
    fun encode(message: SpecificRecordBase, stream: OutputStream) {
        val encoder = getEncoder(message)
        encoder.encode(message, stream)
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

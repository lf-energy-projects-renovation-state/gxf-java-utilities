package com.gxf.utilities.kafka.avro

import org.apache.avro.Schema
import org.apache.avro.specific.SpecificRecordBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AvroSerializerTest {
    private val topic = "topic-yo"

    private val avroSerializer = AvroSerializer()

    @Test
    fun serialize() {
        val message1 = AvroSchema1("field no 1", "field no 2")
        val message2 = AvroSchema2("message in a bottle")
        val message3 = AvroSchema2("another message for you")

        avroSerializer.serialize(topic, message1)
        avroSerializer.serialize(topic, message2)
        avroSerializer.serialize(topic, message3)

        assertThat(avroSerializer.encoders).containsKeys(AvroSchema1::class)
        assertThat(avroSerializer.encoders).containsKeys(AvroSchema2::class)
        assertThat(avroSerializer.encoders.size).isEqualTo(2)
    }
}

class AvroSchema1(private var field1: String, private var field2: String): SpecificRecordBase() {
    override fun getSchema(): Schema = Schema.Parser()
        .parse("{\"type\":\"record\",\"name\":\"AvroSchema1\",\"namespace\":\"com.alliander.gxf.utilities.kafka.avro\",\"fields\":[{\"name\":\"field1\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}, {\"name\":\"field2\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]}")

    override fun put(field: Int, value: Any?) {
        when(field) {
            0 -> {
                if(value != null) {
                    field1 = value.toString()
                }
            }
            1 -> {
                if(value != null) {
                    field2 = value.toString()
                }
            }
            else -> throw IndexOutOfBoundsException()
        }
    }

    override fun get(field: Int): Any {
        return when(field) {
            0 -> field1
            1 -> field2
            else -> throw IndexOutOfBoundsException()
        }
    }
}

class AvroSchema2(private var message: String): SpecificRecordBase() {
    override fun getSchema(): Schema = Schema.Parser()
            .parse("{\"type\":\"record\",\"name\":\"AvroSchema2\",\"namespace\":\"com.alliander.gxf.utilities.kafka.avro\",\"fields\":[{\"name\":\"message\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]}")

    override fun put(field: Int, value: Any?) {
        when(field) {
            0 -> {
                if(value != null) {
                    message = value.toString()
                }
            }
            else -> throw IndexOutOfBoundsException()
        }
    }

    override fun get(field: Int): Any {
        return when(field) {
            0 -> message
            else -> throw IndexOutOfBoundsException()
        }
    }
}

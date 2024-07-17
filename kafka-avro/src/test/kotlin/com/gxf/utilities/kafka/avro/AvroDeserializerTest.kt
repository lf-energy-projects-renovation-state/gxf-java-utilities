package com.gxf.utilities.kafka.avro

import com.alliander.gxf.utilities.kafka.avro.AvroSchema1
import com.alliander.gxf.utilities.kafka.avro.AvroSchema2
import com.alliander.gxf.utilities.kafka.avro.AvroSchema3
import org.apache.kafka.common.errors.SerializationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class AvroDeserializerTest {

    @Test
    fun avroDeserializerTest() {
        val message1 = AvroSchema1("field no 1", "field no 2")
        val message2 = AvroSchema2("message in a bottle")
        val message3 = AvroSchema3("message in a bottle")
        val deserializer = AvroDeserializer(listOf(AvroSchema1.getClassSchema(), AvroSchema2.getClassSchema()))

        assertThat(deserializer.deserialize("topic1", message1.toByteBuffer().array()))
            .isEqualTo(message1)
        assertThat(deserializer.deserialize("topic2", message2.toByteBuffer().array()))
            .isEqualTo(message2)

        assertThatThrownBy({deserializer.deserialize("topic3", message3.toByteBuffer().array())})
            .isInstanceOf(SerializationException::class.java)
            .hasMessageContaining("Error deserializing Avro message for topic: topic3");

    }
}

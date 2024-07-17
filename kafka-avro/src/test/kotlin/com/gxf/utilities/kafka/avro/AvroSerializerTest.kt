package com.gxf.utilities.kafka.avro

import com.alliander.gxf.utilities.kafka.avro.AvroSchema1
import com.alliander.gxf.utilities.kafka.avro.AvroSchema2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AvroSerializerTest {
    @Test
    fun testEncodersCache() {
        val message1 = AvroSchema1("field no 1", "field no 2")
        val message2 = AvroSchema2("message in a bottle")
        val serializer = AvroSerializer()

        serializer.serialize("", message1)
        serializer.serialize("", message2)

        assertThat(AvroEncoder.encoders).containsKeys(AvroSchema1::class)
        assertThat(AvroEncoder.encoders).containsKeys(AvroSchema2::class)
        assertThat(AvroEncoder.encoders.size).isEqualTo(2)
    }
}

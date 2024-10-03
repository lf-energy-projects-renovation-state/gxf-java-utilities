// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.avro

import com.alliander.gxf.utilities.kafka.avro.AvroSchema1
import com.alliander.gxf.utilities.kafka.avro.AvroSchema2
import java.io.ByteArrayOutputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AvroEncoderTest {
    @Test
    fun testEncodersCache() {
        val message1 = AvroSchema1("field no 1", "field no 2")
        val message2 = AvroSchema2("message in a bottle")
        val message3 = AvroSchema2("another message for you")
        val message4 = AvroSchema2("encode to stream!")

        AvroEncoder.encode(message1)
        AvroEncoder.encode(message2)
        AvroEncoder.encode(message3)
        AvroEncoder.encode(message4, ByteArrayOutputStream())

        assertThat(AvroEncoder.encoders).containsKeys(AvroSchema1::class)
        assertThat(AvroEncoder.encoders).containsKeys(AvroSchema2::class)
        assertThat(AvroEncoder.encoders.size).isEqualTo(2)
    }
}

// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.avro

import com.alliander.gxf.utilities.kafka.avro.AvroSchema1
import com.alliander.gxf.utilities.kafka.avro.AvroSchema2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AvroSerializerTest {
    val topicName = "topic-name"

    @Test
    fun `should fill encoder cache`() {
        val message1 = AvroSchema1("field no 1", "field no 2")
        val message2 = AvroSchema2("message in a bottle")
        val serializer = AvroSerializer()

        serializer.serialize(topicName, message1)
        serializer.serialize(topicName, message2)

        assertThat(AvroEncoder.encoders).containsKeys(AvroSchema1::class)
        assertThat(AvroEncoder.encoders).containsKeys(AvroSchema2::class)
        assertThat(AvroEncoder.encoders.size).isEqualTo(2)
    }

    @Test
    fun `should allow a tombstone message`() {
        val serializer = AvroSerializer()

        val result = serializer.serialize(topicName, null)

        assertThat(result).isEqualTo(ByteArray(0))
    }
}

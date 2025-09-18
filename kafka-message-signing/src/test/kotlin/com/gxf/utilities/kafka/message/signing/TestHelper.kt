// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing

import java.util.function.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Header

object TestHelper {
    fun <K, V> producerRecordToConsumerRecord(producerRecord: ProducerRecord<K, V>): ConsumerRecord<K, V> {
        val consumerRecord =
            ConsumerRecord(producerRecord.topic(), 0, 123L, producerRecord.key(), producerRecord.value())
        producerRecord.headers().forEach(Consumer { header: Header? -> consumerRecord.headers().add(header) })
        return consumerRecord
    }
}

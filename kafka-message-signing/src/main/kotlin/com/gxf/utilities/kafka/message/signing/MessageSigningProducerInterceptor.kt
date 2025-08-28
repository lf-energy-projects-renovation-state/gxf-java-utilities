// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing

import org.apache.kafka.clients.producer.ProducerInterceptor
import org.apache.kafka.clients.producer.ProducerRecord

class MessageSigningProducerInterceptor(private val messageSigner: MessageSigner) :
    ProducerInterceptor<String, ByteArray> {
    override fun onSend(producerRecord: ProducerRecord<String, ByteArray>): ProducerRecord<String, ByteArray> =
        messageSigner.signByteArrayRecordUsingHeader(producerRecord)

    override fun onAcknowledgement(metadata: org.apache.kafka.clients.producer.RecordMetadata?, exception: Exception?) {
        // No-op
    }

    override fun close() {
        // No-op
    }

    override fun configure(configs: MutableMap<String, *>?) {
        // No-op
    }
}

// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing

import org.apache.kafka.clients.producer.ProducerInterceptor
import org.apache.kafka.clients.producer.ProducerRecord

class MessageSigningProducerInterceptor() : ProducerInterceptor<String, ByteArray> {
    private lateinit var messageSigner: MessageSigner

    override fun onSend(producerRecord: ProducerRecord<String, ByteArray>): ProducerRecord<String, ByteArray> =
        messageSigner.signByteArrayRecordUsingHeader(producerRecord)

    override fun onAcknowledgement(metadata: org.apache.kafka.clients.producer.RecordMetadata?, exception: Exception?) {
        // No-op
    }

    override fun close() {
        // No-op
    }

    override fun configure(configs: MutableMap<String, *>?) {
        messageSigner = (configs?.get("message.signer") ?: throw Exception()) as MessageSigner
    }
}

// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing.interceptors

import com.gxf.utilities.kafka.message.signing.MessageSigner
import org.apache.kafka.clients.producer.ProducerInterceptor
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata

class MessageSigningByteArrayProducerInterceptor() : ProducerInterceptor<String, ByteArray> {
    private lateinit var messageSigner: MessageSigner

    override fun onSend(producerRecord: ProducerRecord<String, ByteArray>): ProducerRecord<String, ByteArray> =
        messageSigner.signByteArrayRecordUsingHeader(producerRecord)

    override fun onAcknowledgement(metadata: RecordMetadata?, exception: Exception?) {
        // not used
    }

    override fun close() {
        // not used
    }

    override fun configure(configs: MutableMap<String, *>?) {
        messageSigner = (configs?.get("message.signer") ?: throw Exception()) as MessageSigner
    }
}

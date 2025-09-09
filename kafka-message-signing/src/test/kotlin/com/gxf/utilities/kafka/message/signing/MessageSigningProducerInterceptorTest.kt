// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class MessageSigningProducerInterceptorTest {
    @InjectMockKs private lateinit var interceptor: MessageSigningProducerInterceptor
    @MockK private lateinit var messageSigner: MessageSigner

    @Test
    fun testOnSend() {
        val producerRecord = ProducerRecord("topic", "key", "value".toByteArray())
        every { messageSigner.signByteArrayRecordUsingHeader(producerRecord) } answers
            {
                producerRecord.apply { headers().add("signature", "signed".toByteArray()) }
            }

        assertThat(producerRecord.headers()).isEmpty()

        interceptor.onSend(producerRecord)

        assertThat(producerRecord.headers()).isNotEmpty()
        assertThat(producerRecord.headers().lastHeader("signature")).isNotNull
    }
}

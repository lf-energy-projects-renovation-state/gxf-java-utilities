// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message

import com.gxf.utilities.kafka.message.IntegrationTestHelper.createAvroKafkaConsumer
import com.gxf.utilities.kafka.message.IntegrationTestHelper.createAvroKafkaProducer
import com.gxf.utilities.kafka.message.IntegrationTestHelper.createByteArrayKafkaConsumer
import com.gxf.utilities.kafka.message.IntegrationTestHelper.createByteArrayKafkaProducer
import com.gxf.utilities.kafka.message.signing.MessageSigner
import com.gxf.utilities.kafka.message.signing.MessageSigningAutoConfiguration
import java.time.Duration
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext

@SpringBootTest(classes = [MessageSigningAutoConfiguration::class])
@EmbeddedKafka(topics = ["test-topic"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MessageSigningInterceptorIT {
    @Autowired private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker
    @Autowired private lateinit var messageSigner: MessageSigner

    val topic = "test-topic"

    @Test
    fun `can sign ByteArray message with interceptor`() {
        val producer = createByteArrayKafkaProducer(embeddedKafkaBroker, messageSigner)
        val consumer = createByteArrayKafkaConsumer(embeddedKafkaBroker, topic)

        val unsignedRecord = ProducerRecord(topic, "key", "value".toByteArray())
        producer.send(unsignedRecord)

        val records = consumer.poll(Duration.ofSeconds(5)).records(topic)

        assertThat(records).hasSize(1)

        val receivedRecord = records.first()
        val signatureHeader = receivedRecord.headers().lastHeader(MessageSigner.RECORD_HEADER_KEY_SIGNATURE)

        assertThat(signatureHeader).isNotNull
        assertThat(signatureHeader.value()).isNotEmpty()
        assertThat(messageSigner.verifyByteArrayRecordUsingHeader(receivedRecord)).isTrue()
    }

    @Test
    fun `can sign Avro message with interceptor`() {
        val producer = createAvroKafkaProducer(embeddedKafkaBroker, messageSigner)
        val consumer = createAvroKafkaConsumer(embeddedKafkaBroker, topic)

        val unsignedRecord = ProducerRecord<String, SpecificRecordBase>(topic, "key", Message("value"))
        producer.send(unsignedRecord)

        val polled = consumer.poll(Duration.ofSeconds(5))
        val records = polled.records(topic)

        assertThat(records).hasSize(1)

        val receivedRecord = records.first()
        val signatureHeader = receivedRecord.headers().lastHeader(MessageSigner.RECORD_HEADER_KEY_SIGNATURE)

        assertThat(signatureHeader).isNotNull
        assertThat(signatureHeader.value()).isNotEmpty()
    }
}

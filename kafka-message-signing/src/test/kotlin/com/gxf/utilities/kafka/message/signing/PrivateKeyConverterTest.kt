package com.gxf.utilities.kafka.message.signing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PrivateKeyConverterTest {
    @Test
    fun shouldReadPrivateKeyFile() {
        val converter = PrivateKeyConverter()

        val privateKey = converter.convert("classpath:pkcs8/keypair.pem")

        assertThat(privateKey).isNotNull
    }
}

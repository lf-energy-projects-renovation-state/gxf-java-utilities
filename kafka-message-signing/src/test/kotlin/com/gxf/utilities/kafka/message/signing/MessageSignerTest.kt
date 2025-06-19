// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MessageSignerTest {
    @Test
    fun signingCanBeDisabled() {
        val signingDisabledProperties = MessageSigningProperties(signingEnabled = false)
        val messageSignerSigningDisabled = MessageSigner(signingDisabledProperties)

        assertThat(messageSignerSigningDisabled.canSignMessages()).isFalse()
        assertThat(messageSignerSigningDisabled.canVerifyMessageSignatures()).isFalse()
    }
}

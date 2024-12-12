// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.signing

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(MessageSigningProperties::class)
@ComponentScan("com.gxf.utilities.kafka.message.signing")
class MessageSigningAutoConfiguration {
    // Only instantiate when no other bean has been configured
    @ConditionalOnMissingBean
    @Bean
    fun messageSigner(signingProperties: MessageSigningProperties): MessageSigner {
        return MessageSigner(signingProperties)
    }
}

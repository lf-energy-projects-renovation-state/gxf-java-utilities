// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.oslp.message.signing.configuration

import com.gxf.utilities.oslp.message.signing.KeyProvider
import com.gxf.utilities.oslp.message.signing.SigningUtil
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(SigningProperties::class)
@ComponentScan("com.gxf.utilities.oslp.message.signing")
class SigningAutoConfiguration {

    @ConditionalOnMissingBean
    @Bean
    fun signingUtil(signingProperties: SigningProperties, keyProvider: KeyProvider): SigningUtil {
        return SigningUtil(signingProperties, keyProvider)
    }
}

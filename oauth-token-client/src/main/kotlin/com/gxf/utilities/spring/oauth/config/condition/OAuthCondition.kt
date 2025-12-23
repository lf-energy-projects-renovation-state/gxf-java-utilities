// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.spring.oauth.config.condition

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

class OAuthTokenResourceEnabledCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
        oAuthEnabled(context) && tokenLocationPresent(context)
}

class OAuthMsalEnabledCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
        oAuthEnabled(context) && !tokenLocationPresent(context)
}

private fun oAuthEnabled(context: ConditionContext) =
    context.environment.getProperty("oauth.client.enabled").equals("true", ignoreCase = true)

private fun tokenLocationPresent(context: ConditionContext) =
    !context.environment.getProperty("oauth.client.token-location").isNullOrBlank()

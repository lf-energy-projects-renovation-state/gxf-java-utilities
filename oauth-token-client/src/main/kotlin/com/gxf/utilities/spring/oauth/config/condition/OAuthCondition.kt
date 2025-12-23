// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.spring.oauth.config.condition

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

/** Condition to enable or disable the Oauth Client components */
abstract class OAuthCondition : Condition {
    fun oAuthEnabled(context: ConditionContext) =
        context.environment.getProperty("oauth.client.enabled").equals("true", ignoreCase = true)

    fun tokenLocationPresent(context: ConditionContext) =
        !context.environment.getProperty("oauth.client.token-location").isNullOrBlank()
}

class OAuthTokenResourceEnabledCondition : OAuthCondition() {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
        oAuthEnabled(context) && tokenLocationPresent(context)
}

class OAuthMsalEnabledCondition : OAuthCondition() {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
        oAuthEnabled(context) && !tokenLocationPresent(context)
}

class OAuthDisabledCondition : OAuthCondition() {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean = !oAuthEnabled(context)
}

// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.spring.oauth.config.condition

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext

/** Condition to enable or disable the Oauth Client components */
abstract class OAuthCondition : Condition {
    fun oAuthEnabled(context: ConditionContext) =
        context.environment.getProperty("oauth.client.enabled").equals("true", ignoreCase = true)
}

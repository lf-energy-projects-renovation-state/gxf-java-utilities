// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.spring.oauth.config.condition

import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

/** Condition if OAuth is enabled */
class OAuthEnabledCondition : OAuthCondition() {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean = oAuthEnabled(context)
}

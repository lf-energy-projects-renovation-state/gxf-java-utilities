// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "gxf-java-utilities"
include("kafka-azure-oauth")
include("oauth-token-client")
include("kafka-avro")
include("kafka-message-signing")

// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("avro", "1.11.3")

            library("avro", "org.apache.avro", "avro").versionRef("avro")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "gxf-java-utilities"
include("kafka-azure-oauth")
include("oauth-token-client")
include("kafka-avro")
include("kafka-message-signing")

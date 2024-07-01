// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("avro", "1.11.3")
            version("mockitoKotlin", "5.1.0")

            library("avro", "org.apache.avro", "avro").versionRef("avro")
            library(
                "mockitoKotlin",
                "org.mockito.kotlin",
                "mockito-kotlin"
            ).versionRef("mockitoKotlin")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "gxf-java-utilities"
include("kafka-azure-oauth")
include("oauth-token-client")
include("kafka-avro")
include("kafka-message-signing")

// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0


plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "gxf-java-utilities"
include("kafka-azure-oauth")
include("oauth-token-client")
include("kafka-avro")
include("kafka-message-signing")


dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("avro", "1.12.0")
            version("msal4j", "1.16.1")

            library("avro", "org.apache.avro", "avro").versionRef("avro")
            library("msal", "com.microsoft.azure", "msal4j").versionRef("msal4j")
        }

        create("testLibs") {
            version("mockitoKotlin", "5.1.0")
            version("mockServer", "5.15.0")

            library("mockitoKotlin", "org.mockito.kotlin", "mockito-kotlin").versionRef("mockitoKotlin")
            library("mockServer", "org.mock-server", "mockserver-spring-test-listener").versionRef("mockServer")
        }
    }
}

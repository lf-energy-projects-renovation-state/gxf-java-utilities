plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "gxf-java-utilities"
include("kafka-azure-oauth")
include("oauth-token-client")
include("kafka-avro")

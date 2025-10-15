// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(libs.springContext)
    implementation(libs.springKafka)
    implementation(libs.springBootAutoconfigure)
    implementation(libs.kotlinLoggingJvm)

    implementation(project(":kafka-avro"))

    api(libs.avro)

    testImplementation(libs.junitJupiterApi)
    testImplementation(libs.junitJupiterEngine)
    testImplementation(libs.assertJ)
    testImplementation(libs.springTest)
    testImplementation(libs.springBootTest)
    testImplementation(libs.springBootStarter)
    testImplementation(libs.mockk)

    testRuntimeOnly(libs.junitPlatformLauncher)
}

tasks.test {
    useJUnitPlatform()
}

testing {
    suites {
        val integrationTest by registering(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation(libs.springBootStarterTest)
                implementation(libs.springKafka)
                implementation(libs.springKafkaTest)
                implementation(libs.kafkaClients)
                implementation(libs.assertJ)
                implementation(libs.avro)
                implementation(project(":kafka-avro"))
            }
        }
    }
}

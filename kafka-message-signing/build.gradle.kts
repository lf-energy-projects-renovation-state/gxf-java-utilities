// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(libs.springContext)
    implementation(libs.springBootKafka)
    implementation(libs.kotlinLoggingJvm)

    implementation(project(":kafka-avro"))

    api(libs.avro)

    // Used to generate properties metadata
    kapt(libs.springBootConfigurationProcessor)

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
                implementation(libs.springBootKafka)
                implementation(libs.springBootKafkaTest)
                implementation(libs.kafkaClients)
                implementation(libs.assertJ)
                implementation(libs.avro)
                implementation(project(":kafka-avro"))
            }
        }
    }
}

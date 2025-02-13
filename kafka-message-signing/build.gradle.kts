// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(libs.springContext)
    implementation(libs.springKafka)
    implementation(libs.springBootAutoconfigure)

    implementation(project(":kafka-avro"))

    api(libs.avro)

    testImplementation(libs.junitJupiterApi)
    testImplementation(libs.junitJupiterEngine)
    testImplementation(libs.assertJ)
    testImplementation(libs.springTest)
    testImplementation(libs.springBootTest)
    testImplementation(libs.springBootStarter)
    testImplementation(libs.mockitoKotlin)

    testRuntimeOnly(libs.junitPlatformLauncher)
}

tasks.test {
    useJUnitPlatform()
}

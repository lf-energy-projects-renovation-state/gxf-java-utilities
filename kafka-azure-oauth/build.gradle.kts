dependencies {
    implementation(libs.kafkaClients)
    implementation(libs.msal)

    implementation(libs.slf4jApi)

    testImplementation(libs.junitJupiterApi)
    testImplementation(libs.junitJupiterEngine)

    testImplementation(libs.assertJ)

    testRuntimeOnly(libs.junitPlatformLauncher)
}

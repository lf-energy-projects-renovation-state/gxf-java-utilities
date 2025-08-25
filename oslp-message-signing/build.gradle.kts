dependencies {
    implementation(libs.kotlinLoggingJvm)
    implementation(libs.slf4jApi)

    testImplementation(libs.junitJupiterApi)
    testImplementation(libs.mockitoKotlin)
    testImplementation(libs.assertJ)
    testRuntimeOnly(libs.junitJupiterEngine)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

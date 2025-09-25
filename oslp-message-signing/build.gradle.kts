dependencies {
    implementation(libs.slf4jApi)

    testImplementation(libs.junitJupiterApi)
    testImplementation(libs.mockk)
    testImplementation(libs.assertJ)
    testRuntimeOnly(libs.junitJupiterEngine)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

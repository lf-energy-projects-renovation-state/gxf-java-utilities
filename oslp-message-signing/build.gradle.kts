dependencies {
    implementation(libs.springContext)
    implementation(libs.springBootAutoconfigure)
    implementation(libs.kotlinLoggingJvm)
    implementation(libs.slf4jApi)

    testImplementation(libs.junitJupiterApi)
    testImplementation(libs.mockitoKotlin)
    testImplementation(libs.assertJ)
    testRuntimeOnly(libs.junitJupiterEngine)
    testImplementation(libs.springTest)
    testImplementation(libs.springBootTest)
    testImplementation(libs.springBootStarter)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

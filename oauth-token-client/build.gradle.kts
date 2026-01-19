dependencies{
    implementation(libs.springBoot)
    implementation(libs.kotlinReflect)
    implementation(libs.msal)

    // Used to generate properties metadata
    kapt(libs.springBootConfigurationProcessor)

    testImplementation(libs.junitJupiterApi)
    testImplementation(libs.junitJupiterEngine)
    testImplementation(libs.springBootTest)
    testImplementation(libs.assertJ)
    testImplementation(libs.mockk)

    testRuntimeOnly(libs.junitPlatformLauncher)
}

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation(libs.springBootStarterTest)
                implementation(libs.msal)
                implementation(libs.mockk)
                implementation(libs.springmockk)
            }
        }
    }
}

tasks.check {
    dependsOn("integrationTest")
}

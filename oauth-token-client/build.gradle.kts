dependencies{
    implementation(libs.springContext)
    api(libs.msal)

    testImplementation(libs.junitJupiterApi)
    testImplementation(libs.junitJupiterEngine)

    testImplementation(libs.springTest)

    testImplementation(libs.assertJ)

    testRuntimeOnly(libs.junitPlatformLauncher)
}

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation(libs.springBootStarterTest)
            }
        }
    }
}

tasks.check {
    dependsOn("integrationTest")
}

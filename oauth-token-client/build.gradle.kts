dependencies{
    implementation(libs.springBootAutoconfigure)
    implementation(libs.kotlinReflect)
    implementation(libs.msal)

    testImplementation(libs.junitJupiterApi)
    testImplementation(libs.junitJupiterEngine)

    testImplementation(libs.springBootTest)

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
                implementation(libs.msal)
            }
        }
    }
}

tasks.check {
    dependsOn("integrationTest")
}

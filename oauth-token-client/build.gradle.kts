dependencies{
    implementation("org.springframework:spring-context")
    api(libs.msal)

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")

    testImplementation("org.springframework:spring-test")

    testImplementation("org.assertj:assertj-core")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation("org.springframework.boot:spring-boot-starter-test")
                implementation(testLibs.mockServer) {
                    // CVE fixes
                    exclude(group = "org.bouncycastle", module = "bcpkix-jdk18on")
                    exclude(group = "org.bouncycastle", module = "bcprov-jdk18on")
                    exclude(group = "org.bouncycastle", module = "bcutil-jdk18on")
                }
            }
        }
    }
}

tasks.check {
    dependsOn("integrationTest")
}

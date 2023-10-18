dependencies{
    implementation("org.springframework:spring-context")
    api("com.microsoft.azure:msal4j:1.13.10")

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")

    testImplementation("org.springframework:spring-test")

    testImplementation("org.assertj:assertj-core")
}

publishing {
    publications {
        create<MavenPublication>("java") {
            from(components.findByName("java"))
        }
    }
}

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation("org.springframework.boot:spring-boot-starter-test")
                implementation("org.mock-server:mockserver-spring-test-listener:5.15.0")
            }
        }
    }
}

tasks.check {
    dependsOn("integrationTest")
}

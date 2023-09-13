dependencies{
    implementation("org.springframework:spring-context")
    implementation("com.microsoft.azure:msal4j:1.13.10")

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

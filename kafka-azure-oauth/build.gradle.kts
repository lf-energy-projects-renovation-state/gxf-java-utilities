dependencies {
    implementation("org.apache.kafka:kafka-clients")
    implementation("com.microsoft.azure:msal4j:1.13.10")

    implementation("org.slf4j:slf4j-api")

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")

    testImplementation("org.assertj:assertj-core")
}

publishing {
    publications {
        create<MavenPublication>("java") {
            from(components.findByName("java"))
        }
    }
}

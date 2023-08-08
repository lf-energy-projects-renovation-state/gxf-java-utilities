dependencies {
    implementation("org.apache.kafka:kafka-clients")
    implementation("org.apache.avro:avro:1.11.1")

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

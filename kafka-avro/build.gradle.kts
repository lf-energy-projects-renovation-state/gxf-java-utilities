import com.github.davidmc24.gradle.plugin.avro.GenerateAvroJavaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.avro)
}

buildscript {
    configurations.all { resolutionStrategy { force("org.kohsuke:github-api:1.330") } }
    dependencies {
        // override the avro dependencies (1.12.1) of the avro plugin (that contain vulnerabilities)
        classpath(libs.avro)
    }

    configurations {
        classpath {
            resolutionStrategy {
                // Temporary: check regularly if still necessary to override plugin classpath dependencies, and remove
                // when no longer needed
                eachDependency {
                    if (requested.group == "com.fasterxml.jackson.core" && requested.name == "jackson-core") {
                        useVersion("2.21.2")
                        because(
                            "Override plugin classpath to use non-vulnerable com.fasterxml.jackson.core:jackson-core 2.21.2",
                        )
                    }
                }
            }
        }
    }
}

configurations.all {
    resolutionStrategy {
        // Temporary: check regularly if still necessary to override dependency versions, and remove when no longer
        // needed
        eachDependency {
            if (requested.group == "com.fasterxml.jackson.core" && requested.name == "jackson-core") {
                useVersion("2.21.2")
                because(
                    "Override BOM and all constraints to use non-vulnerable com.fasterxml.jackson.core:jackson-core 2.21.2",
                )
            }
        }
    }
}

dependencies {
    implementation(libs.avro)
    implementation(libs.kafkaClients)
    implementation(libs.kotlinLoggingJvm)

    implementation(libs.slf4jApi)

    testImplementation(libs.junitJupiterApi)
    testImplementation(libs.junitJupiterEngine)

    testImplementation(libs.assertJ)

    testRuntimeOnly(libs.junitPlatformLauncher)
}

tasks.withType<KotlinCompile> {
    dependsOn(
        tasks.withType<GenerateAvroJavaTask>(),
    )
}

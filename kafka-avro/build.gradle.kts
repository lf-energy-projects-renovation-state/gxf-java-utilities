import com.github.davidmc24.gradle.plugin.avro.GenerateAvroJavaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.avro)
}

dependencies {
    implementation(libs.kafkaClients)
    implementation(libs.avro)

    implementation(libs.slf4jApi)

    testImplementation(libs.junitJupiterApi)
    testImplementation(libs.junitJupiterEngine)

    testImplementation(libs.assertJ)

    testRuntimeOnly(libs.junitPlatformLauncher)
}

tasks.withType<KotlinCompile> {
    dependsOn(
        tasks.withType<GenerateAvroJavaTask>()
    )
}

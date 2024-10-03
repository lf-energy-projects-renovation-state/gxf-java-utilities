import io.spring.gradle.dependencymanagement.internal.dsl.StandardDependencyManagementExtension
import org.jetbrains.kotlin.com.github.gundy.semver4j.SemVer
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import java.net.URI

plugins {
    id("io.spring.dependency-management") version "1.1.6" apply false
    kotlin("jvm") version "2.0.20" apply false
    kotlin("plugin.spring") version "2.0.20" apply false
    id("org.sonarqube") version "5.1.0.4882"
}

sonar {
    properties {
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.projectKey", "OSGP_gxf-java-utilities")
        property("sonar.organization", "gxf")
    }
}
group = "com.gxf.utilities"
version = System.getenv("GITHUB_REF_NAME")
            ?.replace("/", "-")
            ?.lowercase()
            ?.let { if (SemVer.valid(it)) it.removePrefix("v") else "${it}-SNAPSHOT" }
        ?: "develop"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.gradle.maven-publish")
    apply(plugin = "jacoco")
    apply(plugin = "jacoco-report-aggregation")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    extensions.configure<StandardDependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.4")
        }
    }

    extensions.configure<JavaPluginExtension> {
        withJavadocJar()
        withSourcesJar()
    }

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
        compilerOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    tasks.register<DependencyReportTask>("dependenciesAll"){ group = "help" }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    extensions.configure<PublishingExtension> {
        repositories {
            mavenLocal()
            maven {
                name = "GitHubPackages"
                url = URI("https://maven.pkg.github.com/osgp/gxf-java-utilities")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
        publications {
            create<MavenPublication>("java") {
                from(components.getByName("java"))
            }
        }
    }
}

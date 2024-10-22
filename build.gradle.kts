import com.diffplug.gradle.spotless.SpotlessExtension
import io.spring.gradle.dependencymanagement.internal.dsl.StandardDependencyManagementExtension
import org.jetbrains.kotlin.com.github.gundy.semver4j.SemVer
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    id("io.spring.dependency-management") version "1.1.6" apply false
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.spring") version "2.0.21" apply false
    id("org.sonarqube") version "5.1.0.4882"
    id("com.diffplug.spotless") version("6.25.0")
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
    apply(plugin = "com.diffplug.spotless")

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

    extensions.configure<SpotlessExtension> {
        kotlin {
            // by default the target is every '.kt' and '.kts' file in the java source sets
            ktfmt().dropboxStyle().configure {
                it.setMaxWidth(120)
            }
            licenseHeaderFile(
                "${project.rootDir}/license-template.kt",
                "package")
                .updateYearWithLatest(false)
        }
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

    tasks.register<DependencyReportTask>("dependenciesAll"){
        description = "Displays all dependencies declared in all sub projects"
        group = "help"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.register<Copy>("updateGitHooks") {
        description = "Copies the pre-commit Git Hook to the .git/hooks folder."
        group = "verification"
        from("${project.rootDir}/scripts/pre-commit")
        into("${project.rootDir}/.git/hooks")
    }

    tasks.withType<KotlinCompile> {
        dependsOn(
            tasks.named("updateGitHooks")
        )
    }
}

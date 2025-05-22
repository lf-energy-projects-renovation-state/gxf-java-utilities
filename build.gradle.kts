import com.diffplug.gradle.spotless.SpotlessExtension
import io.spring.gradle.dependencymanagement.internal.dsl.StandardDependencyManagementExtension
import org.jetbrains.kotlin.com.github.gundy.semver4j.SemVer
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    alias(libs.plugins.dependencyManagement) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.spring) apply false
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.spotless)
    alias(libs.plugins.gradleWrapperUpgrade)
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

wrapperUpgrade {
    gradle {
        register("gxf-java-utilities") {
            repo.set("OSGP/gxf-java-utilities")
            baseBranch.set("main")
        }
    }
}

subprojects {
    apply(plugin = rootProject.libs.plugins.kotlin.get().pluginId)
    apply(plugin = rootProject.libs.plugins.spring.get().pluginId)
    apply(plugin = rootProject.libs.plugins.dependencyManagement.get().pluginId)
    apply(plugin = rootProject.libs.plugins.mavenPublish.get().pluginId)
    apply(plugin = rootProject.libs.plugins.spotless.get().pluginId)
    apply(plugin = rootProject.libs.plugins.jacoco.get().pluginId)
    apply(plugin = rootProject.libs.plugins.jacocoReportAggregation.get().pluginId)

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    extensions.configure<StandardDependencyManagementExtension> {
        imports {
            mavenBom(rootProject.libs.springBootDependencies.get().toString())
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
            ktfmt().kotlinlangStyle().configure {
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

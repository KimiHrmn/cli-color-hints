import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.21"
    id("org.jetbrains.intellij.platform") version "2.20.1"
}

group = "io.github.kimihrmn.clicolorhints"
version = "0.1.2"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2026.2.3")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        // Match the Kotlin stdlib bundled with the 2025.3 platform.
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "io.github.kimihrmn.clicolorhints"
        name = "CLI Color Hints"
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "253"
            untilBuild = provider { null }
        }
    }
    buildSearchableOptions = false
}

tasks.test {
    // Run the bundled Kotlin plugin in K2 mode inside fixture tests.
    systemProperty("idea.kotlin.plugin.use.k2", "true")
}

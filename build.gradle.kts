plugins {
    kotlin("jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.rewiew"
version = "0.1.3"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "com.rewiew.autofmt"
        name = "Rewiew Code Formatter"
        version = project.version.toString()
        description = "Autoformat and inspect CSS/JS/Twig on save and pre-commit without Node.js"
        vendor {
            name = "Rewiew"
        }
    }
}

dependencies {
    intellijPlatform {
        phpstorm("2024.3")
        bundledPlugin("com.jetbrains.php")
        bundledPlugin("Git4Idea")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks {
    patchPluginXml {
        sinceBuild.set("243")
    }
}

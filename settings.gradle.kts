pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }

    plugins {
        kotlin("multiplatform") version "1.8.20"
        kotlin("plugin.serialization") version "1.8.20"
        id("org.jetbrains.compose") version "1.4.0"
        id("org.ajoberstar.grgit") version "5.2.0"
    }
}

rootProject.name = "jarvis-battles"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

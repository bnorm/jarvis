plugins {
    kotlin("jvm") version "1.8.20" apply false
    kotlin("kapt") version "1.8.20" apply false
    kotlin("plugin.serialization") version "1.8.20" apply false
    id("com.bnorm.robocode") version "0.1.1" apply false
    id("org.jetbrains.compose") version "1.4.0" apply false
    id("me.champeau.gradle.jmh") version "0.5.2" apply false
    id("de.undercouch.download") version "5.0.1" apply false
}

version = "0.1"

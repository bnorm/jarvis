import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("me.champeau.gradle.jmh") version "0.5.2"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")

    jmh("org.openjdk.jmh:jmh-core:1.34")
    kaptJmh("org.openjdk.jmh:jmh-generator-annprocess:1.34")
}

jmh {
    jvmArgs.plusAssign(listOf("-Djmh.separateClasspathJAR=true"))
    duplicateClassesStrategy = DuplicatesStrategy.WARN
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xopt-in=kotlin.ExperimentalStdlibApi",
            "-Xopt-in=kotlin.time.ExperimentalTime"
        )
    }
}

tasks.test {
    useJUnitPlatform()
}

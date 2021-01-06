import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
    kotlin("kapt") version "1.4.21"
    id("com.bnorm.robocode")
    id("me.champeau.gradle.jmh") version "0.5.2"
}

version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.jakewharton.picnic:picnic:0.5.0")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")

    jmh("org.openjdk.jmh:jmh-core:1.27")
    kaptJmh("org.openjdk.jmh:jmh-generator-annprocess:1.27")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.time.ExperimentalTime",
            "-Xopt-in=kotlin.ExperimentalStdlibApi"
        )
    }
}

tasks.test {
    useJUnitPlatform()
}

robocode {
    robots {
        register("Jarvis") {
            classPath = "bnorm.Jarvis"
            version = project.version.toString()
        }
        register("JarvisT") {
            classPath = "bnorm.JarvisT"
            version = project.version.toString()
            description = "Jarvis - Targeting Only"
        }
        register("JarvisM") {
            classPath = "bnorm.JarvisM"
            version = project.version.toString()
            description = "Jarvis - Movement Only"
        }
    }
}

jmh {
    jvmArgs.plusAssign(listOf("-Djmh.separateClasspathJAR=true"))
    duplicateClassesStrategy = DuplicatesStrategy.WARN
}

/*
 * Gradle source set, configurations, and tasks for running battles.
 */

val battles by sourceSets.registering {
    compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
}

val battlesImplementation by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}

val battlesRuntimeOnly by configurations.getting {
    extendsFrom(configurations.testRuntimeOnly.get())
    extendsFrom(configurations.robocodeRuntime.get())
}

// TODO Should there be a different task for each different battle?
val runBattles by project.tasks.registering(Test::class) {
    dependsOn("robotBin")
    // TODO Download robots for battles? Or is this part of tests?

    description = "Runs Robocode battles"
    group = "battles"

    useJUnitPlatform()
    testClassesDirs = battles.get().output.classesDirs
    classpath = battles.get().runtimeClasspath
}

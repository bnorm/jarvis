import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
    id("com.bnorm.robocode")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.jakewharton.picnic:picnic:0.5.0")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
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
        register("Jarvis TC") {
            classPath = "bnorm.Jarvis"
            version = project.version.toString()
        }
        register("Jarvis MC") {
            classPath = "bnorm.Jarvis"
            version = project.version.toString()
        }
    }
}

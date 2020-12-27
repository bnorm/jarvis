plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api("com.github.jengelman.gradle.plugins:shadow:6.1.0")
}

gradlePlugin {
    plugins {
        create("robocodePlugin") {
            id = "com.bnorm.robocode"
            implementationClass = "com.bnorm.robocode.RobocodePlugin"
        }
    }
}

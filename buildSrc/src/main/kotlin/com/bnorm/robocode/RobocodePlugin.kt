package com.bnorm.robocode

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.internal.impldep.org.bouncycastle.crypto.tls.BulkCipherAlgorithm.idea
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel

class RobocodePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply<JavaLibraryPlugin>()
            apply<ShadowPlugin>()

            val robocode = extensions.create<RobocodeExtension>("robocode")

            val robocodeBuildDir = "$buildDir/robocode"
            val generatedBuildDir = "$robocodeBuildDir/generated"

            afterEvaluate {
                repositories {
                    flatDir {
                        dirs(robocode.installDir.dir("libs"))
                    }
                }
            }

            dependencies {
                val implementation by configurations.getting
                implementation("net.sf.robocode:robocode")
            }

            the<SourceSetContainer>().named("main") {
                resources.srcDirs(file(generatedBuildDir))
            }

            val createVersion by tasks.registering {
                for (robot in robocode.robots) {
                    val properties = mapOf(
                        "robocode.version" to "1.9",
                        "robot.name" to robot.name,
                        "robot.classname" to robot.classPath,
                        "robot.version" to robot.version
                    )

                    val propertiesFile = file("$generatedBuildDir/${robot.name}.properties")
                    propertiesFile.parentFile.mkdirs()
                    propertiesFile.writeText(properties.entries
                        .filter { (_, v) -> v != null }
                        .joinToString(separator = "\n") { (k, v) -> "$k=$v" })
                }
            }

            tasks.named("processResources") {
                dependsOn(createVersion)
            }

            val shadowJar by tasks.named<ShadowJar>("shadowJar") {
                exclude("**/META-INF/**")
                dependencies { exclude(dependency(":robocode")) }
            }

            tasks.register<Sync>("unpack") {
                dependsOn(shadowJar)
                from(zipTree(shadowJar.archiveFile))
                into("$robocodeBuildDir/bin")
            }
        }
    }
}

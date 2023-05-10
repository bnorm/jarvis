import org.ajoberstar.grgit.Commit

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("org.ajoberstar.grgit")
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    js(IR) {
        binaries.executable()
        browser {
            commonWebpackConfig {
                sourceMaps = true
                cssSupport { enabled.set(true) }
                scssSupport { enabled.set(true) }
            }
        }
    }
    sourceSets {
        named("jsMain") {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(compose.material)
                implementation(compose.materialIconsExtended)

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
            }
        }
    }
}

compose.experimental {
    web.application {}
}

val generateBattleData by tasks.registering {
    outputs.file(projectDir.resolve("src/jsMain/resources/battles.json"))
    inputs.dir(projectDir.resolve("battles"))

    data class CommitData(
        val commit: Commit,
        val battles: List<File>,
    )

    doLast {
        val commitData =
            projectDir.resolve("battles").walk()
                .maxDepth(1).drop(1)
                .filter { it.isDirectory }
                .map { commit ->
                    CommitData(
                        commit = grgit.show { this.commit = commit.name }.commit,
                        battles = commit.walkTopDown().filter { it.isFile }.toList(),
                    )
                }
                .sortedBy { it.commit.dateTime }

        projectDir.resolve("src/jsMain/resources/battles.json").writeText(
            commitData.joinToString(prefix = "{\"commits\": [", separator = ",", postfix = "]}") { data ->
                """
                    {
                      "info": {
                        "id": "${data.commit.id}",
                        "abbreviatedId": "${data.commit.abbreviatedId}",
                        "authorName": "${data.commit.author.name}",
                        "authorEmail": "${data.commit.author.email}",
                        "committerName": "${data.commit.committer.name}",
                        "committerEmail": "${data.commit.committer.email}",
                        "dateTime": "${data.commit.dateTime.toInstant()}",
                        "shortMessage": "${data.commit.shortMessage}",
                        "fullMessage": "${data.commit.fullMessage.trim().replace("\n", "\\n")}"
                      },
                      "battles": [${data.battles.joinToString { it.readText() }}]
                    }
                """.trimIndent()
            }
        )
    }
}
tasks.named("jsProcessResources").configure { dependsOn(generateBattleData) }

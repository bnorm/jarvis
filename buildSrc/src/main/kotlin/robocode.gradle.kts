plugins {
    `java-library`
    id("com.github.johnrengelman.shadow")
}

repositories {
    flatDir { dirs("lib") }
}

dependencies {
    implementation("net.sf.robocode:robocode:1.9.3.9")
}

sourceSets.main { resources.srcDirs(file("$buildDir/generated")) }

val createVersion = tasks.register("createVersion") {
    val robotClassname = project.property("robot.classname").toString()
    val properties = file("$buildDir/generated/${robotClassname.substringAfterLast('.')}.properties")
    properties.parentFile.mkdirs()
    properties.writeText(
        """
        |robocode.version=1.9
        |robot.version=${version}
        |robot.classname=$robotClassname
        |robot.name=${robotClassname.substringAfterLast('.')}
        """.trimMargin()
    )
}

tasks.processResources { dependsOn(createVersion) }

tasks.shadowJar {
    dependencies { exclude(dependency(":robocode")) }
}

tasks.register("unpack", Sync::class.java) {
    dependsOn(tasks.shadowJar)
    from(zipTree(tasks.shadowJar.get().archiveFile))
    into("$buildDir/bin")
}

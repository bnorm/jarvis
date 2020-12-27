package com.bnorm.robocode

import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.domainObjectContainer
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue
import java.io.File

open class RobocodeExtension(
    objects: ObjectFactory,
    layout: ProjectLayout,
    providerFactory: ProviderFactory
) {
    var installDir: Directory by objects.directoryProperty().apply {
        val os = OperatingSystem.current()
        val robocodeHomeDir = if (os.isWindows) {
            providerFactory.provider { File("/") }
                .forUseAtConfigurationTime()
                .map { File(it, "robocode") }
        } else {
            providerFactory.systemProperty("user.home")
                .forUseAtConfigurationTime()
                .map { File(it, "robocode") }
        }
        convention(layout.dir(robocodeHomeDir))
    }

    val robots = objects.domainObjectContainer(RobocodeRobot::class) { RobocodeRobot(it) }
}

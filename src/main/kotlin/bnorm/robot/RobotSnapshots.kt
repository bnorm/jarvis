package bnorm.robot

import bnorm.parts.gun.virtual.Wave
import bnorm.parts.gun.virtual.WaveContext

class RobotSnapshots(
    var latest: RobotSnapshot
) {
    fun interface Factory {
        fun create(scan: RobotScan, prevSnapshot: RobotSnapshot?): RobotSnapshot
    }

    class Configuration {
        lateinit var factory: Factory
    }

    companion object : RobotContext.Feature<Configuration, RobotSnapshots>, WaveContext.Feature<RobotSnapshot> {
        override suspend fun RobotService.install(robot: Robot, block: Configuration.() -> Unit): RobotSnapshots {
            val configuration = Configuration().apply(block)
            val factory = configuration.factory
            val snapshots = RobotSnapshots(factory.create(robot.latest, null))

            var prevSnapshot: RobotSnapshot? = snapshots.latest
            robot.onScan { scan ->
                val snapshot = factory.create(scan, prevSnapshot)
                snapshot.prev = prevSnapshot
                prevSnapshot = snapshot
                snapshots.latest = snapshot
            }
            robot.onDeath { prevSnapshot = null }

            return snapshots
        }
    }
}

val Robot.snapshot: RobotSnapshot get() = context[RobotSnapshots].latest
val Wave.snapshot: RobotSnapshot get() = context[RobotSnapshots]

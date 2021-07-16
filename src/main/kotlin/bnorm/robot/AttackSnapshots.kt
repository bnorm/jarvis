package bnorm.robot

import bnorm.parts.gun.virtual.Wave
import bnorm.parts.gun.virtual.WaveContext

class AttackSnapshots(
    var latest: RobotSnapshot
) {
    fun interface Factory {
        fun create(scan: RobotScan, prevSnapshot: RobotSnapshot?): RobotSnapshot
    }

    class Configuration {
        lateinit var factory: Factory
    }

    companion object : RobotContext.Feature<Configuration, RobotSnapshots>, WaveContext.Key<RobotSnapshot> {
        override suspend fun RobotService.install(robot: Robot, block: Configuration.() -> Unit): RobotSnapshots {
            val configuration = Configuration().apply(block)
            val factory = configuration.factory
            val snapshots = RobotSnapshots(factory.create(self.latest, null))

            var prevSnapshot: RobotSnapshot? = snapshots.latest
            robot.onScan { scan ->
                val snapshot = factory.create(self.latest, prevSnapshot)
                snapshot.prev = prevSnapshot
                prevSnapshot = snapshot
                snapshots.latest = snapshot
            }
            robot.onDeath { prevSnapshot = null }

            return snapshots
        }
    }
}

val Robot.attackSnapshot: RobotSnapshot get() = context[AttackSnapshots].latest
val Wave.attackSnapshot: RobotSnapshot get() = context[AttackSnapshots]

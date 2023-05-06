package bnorm.robot

import bnorm.plugin.Context
import bnorm.plugin.Plugin
import bnorm.plugin.get

class AttackSnapshots(
    var latest: RobotSnapshot
) {
    fun interface Factory {
        fun create(scan: RobotScan, prevSnapshot: RobotSnapshot?): RobotSnapshot
    }

    class Configuration {
        lateinit var self: Robot
        lateinit var factory: Factory
    }

    companion object : Plugin<Robot, Configuration, RobotSnapshots> {
        override val key = Context.Key<RobotSnapshots>("AttackSnapshot")

        override fun install(holder: Robot, configure: Configuration.() -> Unit): RobotSnapshots {
            val configuration = Configuration().apply(configure)
            val self = configuration.self
            val factory = configuration.factory
            val snapshots = RobotSnapshots(factory.create(self.latest, null))

            var prevSnapshot: RobotSnapshot? = snapshots.latest
            holder.onScan { scan ->
                val snapshot = factory.create(self.latest, prevSnapshot)
                snapshot.prev = prevSnapshot
                prevSnapshot = snapshot
                snapshots.latest = snapshot
            }
            holder.onDeath { prevSnapshot = null }

            return snapshots
        }
    }
}

val Robot.attackSnapshot: RobotSnapshot get() = this[AttackSnapshots].latest

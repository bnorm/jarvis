package bnorm.robot

import bnorm.plugin.Context
import bnorm.plugin.Plugin
import bnorm.plugin.get

class RobotSnapshots(
    var latest: RobotSnapshot
) {
    fun interface Factory {
        fun create(scan: RobotScan, prevSnapshot: RobotSnapshot?): RobotSnapshot
    }

    class Configuration {
        lateinit var factory: Factory
    }

    companion object : Plugin<Robot, Configuration, RobotSnapshots> {
        override val key = Context.Key<RobotSnapshots>("RobotSnapshots")

        override fun install(holder: Robot, configure: Configuration.() -> Unit): RobotSnapshots {
            val configuration = Configuration().apply(configure)
            val factory = configuration.factory
            val snapshots = RobotSnapshots(factory.create(holder.latest, null))

            var prevSnapshot: RobotSnapshot? = snapshots.latest
            holder.onScan { scan ->
                val snapshot = factory.create(scan, prevSnapshot)
                snapshot.prev = prevSnapshot
                prevSnapshot = snapshot
                snapshots.latest = snapshot
            }
            holder.onDeath { prevSnapshot = null }

            return snapshots
        }
    }
}

val Robot.snapshot: RobotSnapshot get() = this[RobotSnapshots].latest

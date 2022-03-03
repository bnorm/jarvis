package bnorm.parts.gun.virtual

import bnorm.Vector
import bnorm.parts.gun.Prediction
import bnorm.plugin.Context
import bnorm.plugin.Plugin
import bnorm.plugin.get
import bnorm.robot.Robot
import bnorm.trace
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class VirtualGuns(
    private val source: Robot,
    private val target: Robot,
    val guns: List<VirtualGun>,
) {
    init {
        require(guns.isNotEmpty())
    }

    class Configuration {
        lateinit var self: Robot
        var predictions: Map<String, Prediction>? = null
    }

    companion object : Plugin<Robot, Configuration, VirtualGuns> {
        override val key = Context.Key<VirtualGuns>("VirtualGuns")
        override suspend fun install(holder: Robot, configure: Configuration.() -> Unit): VirtualGuns {
            val configuration = Configuration().apply(configure)
            val self = configuration.self
            val predictions = configuration.predictions!!
            val guns = predictions.map { (k, v) -> VirtualGun(self, holder, k, v) }

            holder.onScan { scan ->
                guns.forEach { it.scan(scan) }
            }

            holder.onDeath {
                guns.forEach { it.death() }
            }

            return VirtualGuns(self, holder, guns)
        }
    }

    inline fun <reified T : Prediction> prediction(): List<T> {
        return guns.map { it.prediction }.filterIsInstance<T>()
    }

    val best: Prediction get() = guns.maxByOrNull { it.success }!!.prediction

    suspend fun fire(power: Double): Vector {
        if (power <= 0.0) return target.latest.location - source.latest.location

        return coroutineScope {
            guns.map { gun ->
                gun.success to async {
                    trace("gun-${gun.name}") { gun.fire(power) }
                }
            }
        }.maxByOrNull { it.first }!!.second.await()
    }
}

val Robot.virtualGuns get() = this[VirtualGuns]

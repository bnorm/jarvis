package bnorm.parts.gun.virtual

import bnorm.Vector
import bnorm.parts.gun.Prediction
import bnorm.robot.Robot
import bnorm.robot.RobotContext
import bnorm.robot.RobotService
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
        var predictions: List<Prediction>? = null
    }

    companion object : RobotContext.Feature<Configuration, VirtualGuns> {
        override suspend fun RobotService.install(robot: Robot, block: Configuration.() -> Unit): VirtualGuns {
            val configuration = Configuration().apply(block)
            val predictions = configuration.predictions!!
            val guns = predictions.map { VirtualGun(self, robot, it) }

            robot.onScan { scan ->
                guns.forEach { it.scan(scan) }
            }

            robot.onDeath {
                guns.forEach { it.death() }
            }

            return VirtualGuns(self, robot, guns)
        }
    }

    inline fun <reified T : Prediction> prediction(): List<T> {
        return guns.map { it.prediction }.filterIsInstance<T>()
    }

    val best: Prediction get() = guns.maxByOrNull { it.success }!!.prediction

    suspend fun fire(power: Double): Vector {
        if (power <= 0.0) return target.latest.location - source.latest.location

        return coroutineScope {
            guns.map { gun -> gun.success to async { gun.fire(power) } }
        }.maxByOrNull { it.first }!!.second.await()
    }
}

val Robot.guns get() = context[VirtualGuns]

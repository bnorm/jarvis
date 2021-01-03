package bnorm.parts.gun.virtual

import bnorm.Vector
import bnorm.parts.gun.Prediction
import bnorm.robot.Robot
import bnorm.robot.RobotContext
import bnorm.robot.RobotService

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
        override fun RobotService.install(robot: Robot, block: Configuration.() -> Unit): VirtualGuns {
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

    inline fun <reified T: Prediction> prediction(): T? {
        for (gun in guns) {
            if (gun.prediction is T) {
                return gun.prediction
            }
        }
        return null
    }

    fun fire(power: Double): Vector {
        if (power <= 0.0) return target.latest.location - source.latest.location

        var success = guns[0].success

        var max: Vector = guns[0].fire(power)
        for (i in 1 until guns.size) {
            val gun = guns[i]

            val v = gun.fire(power)
            if (gun.success > success) {
                max = v
                success = gun.success
            }
        }
        return max
    }
}

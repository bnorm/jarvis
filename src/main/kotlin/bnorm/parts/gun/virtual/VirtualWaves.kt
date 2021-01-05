package bnorm.parts.gun.virtual

import bnorm.r2
import bnorm.robot.Robot
import bnorm.robot.RobotContext
import bnorm.robot.RobotService
import bnorm.sqr
import robocode.Rules
import java.util.*

class VirtualWaves<T>(
    private val source: Robot,
    private val target: Robot,
    private val listeners: List<Wave.Listener<T>>,
) {
    private val _waves = LinkedList<Wave<T>>()
    val waves: List<Wave<T>> get() = _waves

    init {
        target.onScan { scan ->
            _waves.removeIf { wave ->
                if (wave.origin.r2(scan.location) <= sqr(wave.radius(scan.time))) {
                    listeners.forEach { it.onWave(wave) }
                    true
                } else {
                    false
                }
            }
        }
        target.onDeath {
            _waves.clear()
        }
    }

    class Configuration<T> {
        val listeners = mutableListOf<Wave.Listener<T>>()

        fun listen(listener: Wave.Listener<T>) {
            listeners.add(listener)
        }
    }

    abstract class Feature<T> : RobotContext.Feature<Configuration<T>, VirtualWaves<T>> {
        override fun RobotService.install(robot: Robot, block: Configuration<T>.() -> Unit): VirtualWaves<T> {
            val configuration = Configuration<T>().apply(block)
            return VirtualWaves(self, robot, configuration.listeners)
        }
    }

    fun fire(power: Double, data: T) {
        if (power <= 0.0) return

        val selfLatest = source.latest
        val wave = Wave(
            origin = selfLatest.location,
            speed = Rules.getBulletSpeed(power),
            time = selfLatest.time,
            value = data
        )

        _waves.add(wave)
    }
}

package bnorm.parts.gun.virtual

import bnorm.r2
import bnorm.robot.Robot
import bnorm.sqr
import robocode.Rules
import java.util.*

class RobotWaveManager<T>(
    private val source: Robot,
    private val target: Robot,
) {
    private val listeners = mutableListOf<Wave.Listener<T>>()

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

    fun fire(power: Double, value: T) {
        if (power <= 0.0) return

        val selfLatest = source.latest
        val wave = Wave(
            origin = selfLatest.location,
            speed = Rules.getBulletSpeed(power),
            time = selfLatest.time,
            value = value
        )

        _waves.add(wave)
    }

    fun listen(listener: Wave.Listener<T>) {
        listeners.add(listener)
    }
}

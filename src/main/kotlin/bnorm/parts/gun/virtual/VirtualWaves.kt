package bnorm.parts.gun.virtual

import bnorm.r2
import bnorm.robot.Robot
import bnorm.robot.RobotContext
import bnorm.robot.RobotService
import bnorm.sqr
import robocode.Rules
import java.util.*

typealias WaveListener = suspend (wave: Wave) -> Unit

class VirtualWaves(
    private val source: Robot,
    private val target: Robot,
    private val onWave: suspend Wave.() -> Unit,
    private val listeners: List<WaveListener>,
) {
    private val _waves = LinkedList<Wave>()
    val waves: List<Wave> get() = _waves

    init {
        target.onScan { scan ->
            val iter = _waves.iterator()
            while (iter.hasNext()) {
                val wave = iter.next()
                if (wave.origin.r2(scan.location) <= sqr(wave.radius(scan.time))) {
                    listeners.forEach { it.invoke(wave) }
                    iter.remove()
                }
            }
        }
        target.onDeath {
            _waves.clear()
        }
    }

    class Configuration {
        var onWave: (suspend Wave.() -> Unit)? = null

        val listeners = mutableListOf<WaveListener>()

        fun listen(listener: WaveListener) {
            listeners.add(listener)
        }
    }

    companion object Feature : RobotContext.Feature<Configuration, VirtualWaves> {
        override suspend fun RobotService.install(robot: Robot, block: Configuration.() -> Unit): VirtualWaves {
            val configuration = Configuration().apply(block)
            return VirtualWaves(self, robot, configuration.onWave ?: {}, configuration.listeners)
        }
    }

    suspend fun fire(power: Double, block: suspend Wave.() -> Unit): Wave? {
        if (power <= 0.0) return null

        val selfLatest = source.latest
        val wave = Wave(
            origin = selfLatest.location,
            speed = Rules.getBulletSpeed(power),
            time = selfLatest.time,
            context = WaveContext()
        )
        wave.block()
        wave.onWave()
        _waves.add(wave)
        return wave
    }
}

val Robot.waves get() = context[VirtualWaves]

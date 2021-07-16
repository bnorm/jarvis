package bnorm.parts.gun.virtual

import bnorm.r2
import bnorm.robot.Robot
import bnorm.robot.RobotContext
import bnorm.robot.RobotService
import bnorm.robot.snapshot.BulletSnapshot
import bnorm.sqr
import robocode.Rules
import java.util.*

typealias WaveListener = suspend (wave: Wave) -> Unit
typealias BulletListener = suspend (wave: Wave, bullet: BulletSnapshot) -> Unit

class VirtualWaves(
    private val source: Robot,
    private val target: Robot,
    private val onWave: suspend Wave.() -> Unit,
    private val waveListeners: List<WaveListener>,
    private val bulletListeners: List<BulletListener>,
) {
    private val _waves = LinkedList<Wave>()
    val waves: List<Wave> get() = _waves

    init {
        target.onScan { scan ->
            val iter = _waves.iterator()
            while (iter.hasNext()) {
                val wave = iter.next()
                if (wave.origin.r2(scan.location) <= sqr(wave.radius(scan.time))) {
                    waveListeners.forEach { it.invoke(wave) }
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

        val waveListeners = mutableListOf<WaveListener>()

        fun listen(listener: WaveListener) {
            waveListeners.add(listener)
        }

        val bulletListeners = mutableListOf<BulletListener>()

        fun listen(listener: BulletListener) {
            bulletListeners.add(listener)
        }
    }

    companion object Feature : RobotContext.Feature<Configuration, VirtualWaves> {
        override suspend fun RobotService.install(robot: Robot, block: Configuration.() -> Unit): VirtualWaves {
            val configuration = Configuration().apply(block)
            return VirtualWaves(
                source = self,
                target = robot,
                onWave = configuration.onWave ?: {},
                waveListeners = configuration.waveListeners,
                bulletListeners = configuration.bulletListeners
            )
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

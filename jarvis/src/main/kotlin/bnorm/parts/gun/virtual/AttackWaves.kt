package bnorm.parts.gun.virtual

import bnorm.plugin.Context
import bnorm.plugin.Plugin
import bnorm.plugin.get
import bnorm.r2
import bnorm.robot.Robot
import bnorm.robot.RobotScan
import bnorm.robot.damage
import bnorm.robot.snapshot.BulletSnapshot
import bnorm.sqr
import robocode.Rules
import java.util.*

class AttackWaves(
    private val source: Robot,
    private val target: Robot,
    private val onWave: Wave.(real: Boolean) -> Unit,
    private val waveListeners: List<WaveListener>,
    private val bulletListeners: List<BulletListener>,
) {
    private val _waves = LinkedList<Wave>()
    private val echo = LinkedList<Wave>()
    val waves: List<Wave> get() = _waves

    private var _predicted: Wave? = null
    val predicted: Wave? get() = _predicted

    init {
        target.onScan { scan ->
            val iter = _waves.iterator()
            while (iter.hasNext()) {
                val wave = iter.next()
                if (wave.origin.r2(scan.location) <= sqr(wave.radius(scan.time))) {
                    waveListeners.forEach { it.invoke(wave) }
                    iter.remove()
                    echo.addFirst(wave)
                }
            }

            while (echo.size > 10) {
                echo.removeLast()
            }
        }
        target.onDeath {
            _waves.clear()
        }
    }

    class Configuration {
        lateinit var self: Robot

        var onWave: (Wave.(real: Boolean) -> Unit)? = null

        val waveListeners = mutableListOf<WaveListener>()

        fun listen(listener: WaveListener) {
            waveListeners.add(listener)
        }

        val bulletListeners = mutableListOf<BulletListener>()

        fun listen(listener: BulletListener) {
            bulletListeners.add(listener)
        }
    }

    companion object : Plugin<Robot, Configuration, AttackWaves> {
        override val key = Context.Key<AttackWaves>("AttackWave")
        override fun install(holder: Robot, configure: Configuration.() -> Unit): AttackWaves {
            val configuration = Configuration().apply(configure)
            val attackWaves = AttackWaves(
                source = holder,
                target = configuration.self,
                onWave = configuration.onWave ?: {},
                waveListeners = configuration.waveListeners,
                bulletListeners = configuration.bulletListeners
            )

            var prev: RobotScan? = null
            var lastPower = 3.0
            var lastHeat = 3.0
            var lastFire = 0L
            holder.onScan { scan ->
                prev?.let { prev ->
                    val deltaEnergy = prev.energy - (scan.energy + scan.damage)
                    if (scan.time == prev.time + 1 && (scan.time - lastFire) * 0.1 >= lastHeat && deltaEnergy in 0.1..3.0) {
                        attackWaves.fire(true, deltaEnergy, scan.time - 1)
                        lastPower = deltaEnergy
                        lastHeat = Rules.getGunHeat(deltaEnergy)
                        lastFire = scan.time
                    }
                }
                attackWaves.predict(lastPower, scan.time)
                prev = scan

                if (scan.bulletHit != null) {
                    println("searching for matching wave...")
                    bulletHit(attackWaves, scan.bulletHit, scan.time)
                }
            }
            holder.onDeath {
                prev = null
                lastPower = 3.0
                lastHeat = 3.0
                lastFire = 0L
            }


            return attackWaves
        }

        private fun bulletHit(attackWaves: AttackWaves, bullet: BulletSnapshot, time: Long) {
            for (wave in attackWaves._waves) {
                val bulletDistance = wave.origin.r2(bullet.location)
                val range = sqr(wave.radius(time - 1))..sqr(wave.radius(time + 1))
                println("   b=$bulletDistance w=$range -> ${bulletDistance in range}")
                if (bulletDistance in range) {
                    attackWaves.bulletListeners.forEach { it.invoke(wave, bullet) }
                    return
                }
            }
            for (wave in attackWaves.echo) {
                val bulletDistance = wave.origin.r2(bullet.location)
                val range = sqr(wave.radius(time - 1))..sqr(wave.radius(time + 1))
                println("   b=$bulletDistance w=$range -> ${bulletDistance in range}")
                if (bulletDistance in range) {
                    attackWaves.bulletListeners.forEach { it.invoke(wave, bullet) }
                    return
                }
            }
        }
    }

    private fun predict(power: Double, time: Long) {
        if (power <= 0.0) return

        val latest = source.latest
        val wave = Wave(
            origin = latest.location,
            speed = Rules.getBulletSpeed(power),
            time = time,
        )
        wave.onWave(false)
        _predicted = wave
    }

    private fun fire(real: Boolean, power: Double, time: Long): Wave? {
        if (power <= 0.0) return null

        val latest = source.latest
        val wave = Wave(
            origin = latest.location,
            speed = Rules.getBulletSpeed(power),
            time = time,
        )
        wave.onWave(real)
        _waves.add(wave)
        return wave
    }
}

val Robot.attackWaves get() = this[AttackWaves]

package bnorm.parts.gun.virtual

import bnorm.Polar
import bnorm.Vector
import bnorm.geo.contains
import bnorm.parts.gun.Prediction
import bnorm.parts.tank.TANK_SIZE
import bnorm.r
import bnorm.robot.Robot
import bnorm.robot.RobotScan
import robocode.Rules
import java.util.*
import kotlin.math.sqrt

class VirtualGun(
    private val source: Robot,
    private val target: Robot,
    val name: String,
    val prediction: Prediction,
) {
    companion object {
        private val TANK_HIT_RADIUS = sqrt(2.0) * TANK_SIZE / 2
        private const val ROLLING = 100
    }

    class VirtualBullet(
        val startTime: Long,
        val startLocation: Vector.Cartesian,
        val velocity: Vector.Polar,
        val speed: Double,
    ) {
        fun location(time: Long): Vector.Cartesian {
            return startLocation + velocity * (time - this.startTime).toDouble()
        }

        fun radius(time: Long): Double {
            return speed * (time - startTime)
        }
    }

    private val _bullets = LinkedList<VirtualBullet>()
    val bullets: List<VirtualBullet> get() = _bullets

    private var _latest: Vector? = null
    val latest: Vector get() = _latest!!

    var fired: Long = 0
        private set
    var hit: Long = 0
        private set
    var success: Double = 0.0
        private set

    private fun increment(hit: Boolean) {
        if (hit) this.hit++
        fired++
//        if (fired > ROLLING) {
//            // Rolling average over the last 2000
//            success = ((ROLLING - 1) * success + if (hit) 1 else 0) / ROLLING
//        } else {
        success = this.hit.toDouble() / fired
//        }
    }

    fun fire(power: Double): Vector {
        val speed = Rules.getBulletSpeed(power)
        val velocity = Polar(prediction.predict(power).theta, speed)
        _bullets.addLast(VirtualBullet(source.latest.time, source.latest.location, velocity, speed))
        return velocity
    }

    fun scan(scan: RobotScan) {
        val time = scan.time

        val iterator = _bullets.listIterator()
        while (iterator.hasNext()) {
            val bullet = iterator.next()
            if (bullet.location(time) in scan.tank) {
                // hit
                increment(hit = true)
                iterator.remove()
            } else if (bullet.radius(time) - bullet.startLocation.r(scan.location) > TANK_SIZE) {
                // miss
                increment(hit = false)
                iterator.remove()
            }
        }
    }

    fun death() {
        _bullets.clear()
    }
}

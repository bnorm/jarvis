package bnorm.parts.gun.virtual

import bnorm.Polar
import bnorm.Vector
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
    val prediction: Prediction,
) {
    companion object {
        private val TANK_HIT_RADIUS = sqrt(2.0) * TANK_SIZE / 2
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
        if (fired > 2000) {
            // Rolling average over the last 2000
            success = (1999 * success + if (hit) 1 else 0) / 2000
        } else {
            success = this.hit.toDouble() / fired
        }
    }

    fun fire(power: Double): Vector {
        val speed = Rules.getBulletSpeed(power)
        val velocity = Polar(prediction.predict(target, power).theta, speed)
        _bullets.addLast(VirtualBullet(source.latest.time + 1, source.latest.location, velocity, speed))
        return velocity
    }

    fun scan(scan: RobotScan) {
        val x = scan.location.x
        val y = scan.location.y
        val time = scan.time

        val iterator = _bullets.listIterator()
        while (iterator.hasNext()) {
            val bullet = iterator.next()
            val distance = bullet.startLocation.r(x, y) - bullet.radius(time)
            if (distance < -TANK_HIT_RADIUS) {
                // miss
                increment(hit = false)
                iterator.remove()
            } else if (distance < TANK_HIT_RADIUS) {
                val bulletLocation = bullet.location(time)
                if (x in (bulletLocation.x - TANK_SIZE / 2)..(bulletLocation.x + TANK_SIZE / 2) &&
                    y in (bulletLocation.y - TANK_SIZE / 2)..(bulletLocation.y + TANK_SIZE / 2)
                ) {
                    // hit
                    increment(hit = true)
                    iterator.remove()
                }
            }
        }
    }

    fun death() {
        _bullets.clear()
    }
}

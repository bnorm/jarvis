package bnorm.parts.gun

import bnorm.Polar
import bnorm.Vector
import bnorm.parts.tank.TANK_SIZE
import bnorm.r
import bnorm.robot.RobotScan
import robocode.Rules
import java.util.*
import kotlin.math.sqrt

class VirtualGun {
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

    var fired: Long = 0
        private set
    var hit: Long = 0
        private set
    var success: Double = 0.0
        private set

    private fun increment(hit: Boolean) {
        if (hit) this.hit++
        fired++
        success = this.hit.toDouble() / fired
    }

    fun fire(
        time: Long,
        location: Vector.Cartesian,
        velocity: Vector,
        power: Double,
    ) {
        val speed = Rules.getBulletSpeed(power)
        _bullets.addLast(VirtualBullet(time + 1, location, Polar(velocity.theta, speed), speed))
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

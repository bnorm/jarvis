package bnorm.parts.tank

import bnorm.Polar
import bnorm.Vector
import bnorm.r
import bnorm.robot.Robot
import bnorm.theta
import robocode.util.Utils
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.sign

class OrbitMovement(
    private val target: Robot,
    private val radius: Double,
    private val direction: Double
) : Movement {
    override suspend fun invoke(location: Vector.Cartesian, velocity: Vector.Polar): Vector.Polar {
        val heading = velocity.theta
        val theta = location.theta(target.latest.location)
        val distance = location.r(target.latest.location)
        val bearingOffset = (PI / 2) * (radius / distance).coerceIn(0.5, 1.5) - atan(abs(velocity.r / 2) / radius)

        val clockwise = theta - bearingOffset
        val counter = theta + bearingOffset

        fun closestBearing(heading: Double, h1: Double, h2: Double): Double {
            val b1 = Utils.normalRelativeAngle(h1 - heading)
            val b2 = Utils.normalRelativeAngle(h2 - heading)
            return if (abs(b1) < abs(b2)) b1 else b2
        }

        val movement = sign(direction) * 10 * TANK_MAX_SPEED
        return if (movement < 0.0) {
            Polar(closestBearing(heading + PI, clockwise, counter), movement)
        } else {
            Polar(closestBearing(heading, clockwise, counter), movement)
        }
    }
}

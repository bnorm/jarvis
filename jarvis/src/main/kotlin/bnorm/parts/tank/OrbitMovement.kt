package bnorm.parts.tank

import bnorm.Polar
import bnorm.Vector
import bnorm.draw.Debug
import bnorm.draw.DebugKey
import bnorm.drawCircle
import bnorm.drawProbe
import bnorm.fillCircle
import bnorm.geo.Angle
import bnorm.geo.atan
import bnorm.minBearing
import bnorm.r
import bnorm.robot.Robot
import bnorm.theta
import java.awt.Color
import kotlin.math.abs
import kotlin.math.sign

class OrbitMovement(
    private val target: Robot,
    private val radius: Double,
    private val direction: Double
) : Movement {
    override fun invoke(location: Vector.Cartesian, velocity: Vector.Polar): Vector.Polar {
        val targetLocation = target.latest.location
        val theta = location.theta(targetLocation)
        val distance = location.r(targetLocation)
        val bearingOffset = Angle.QUARTER_CIRCLE * (radius / distance).coerceIn(0.75, 1.25) - atan(abs(velocity.r / 2) / radius)

        val clockwise = theta - bearingOffset
        val counter = theta + bearingOffset

        val movement = sign(direction) * 10 * TANK_MAX_SPEED
        val heading = velocity.theta + if (movement < 0.0) Angle.HALF_CIRCLE else Angle.ZERO
        val bearing = minBearing(heading, clockwise, counter)

        Debug.onDraw(DebugKey.OrbitMovement) {
            color = Color.BLUE
            fillCircle(targetLocation, 8)
            drawCircle(targetLocation, radius)
            drawProbe(location, Polar(clockwise, movement))
            drawProbe(location, Polar(counter, movement))
        }

        return Polar(bearing, movement)
    }
}

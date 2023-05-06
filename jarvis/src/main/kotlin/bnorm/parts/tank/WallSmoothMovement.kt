package bnorm.parts.tank

import bnorm.Polar
import bnorm.Vector
import bnorm.draw.Debug
import bnorm.draw.DebugKey
import bnorm.drawCircle
import bnorm.drawLine
import bnorm.drawProbe
import bnorm.geo.*
import bnorm.minBearing
import bnorm.parts.BattleField
import bnorm.sim.ANGLE_DOWN
import bnorm.sim.ANGLE_LEFT
import bnorm.sim.ANGLE_RIGHT
import bnorm.sim.getTankTurnRate
import java.awt.Color
import kotlin.math.abs
import kotlin.math.tan

class WallSmoothMovement(
    private val battleField: BattleField,
    private val movement: Movement,
) : Movement {
    companion object {
        private fun radius(speed: Double): Double {
            val turn = getTankTurnRate(TANK_MAX_SPEED)
            return (speed / 2) / tan(turn.radians / 2)
        }

        val TANK_MAX_RADIUS = radius(TANK_MAX_SPEED)
        const val WALL_TANK_BUFFER = TANK_SIZE / 2 + 1
    }

    override fun invoke(location: Vector.Cartesian, velocity: Vector.Polar): Vector.Polar {
        return smooth(location, velocity, movement.invoke(location, velocity))
    }

    private fun smooth(
        location: Vector.Cartesian,
        velocity: Vector.Polar,
        movement: Vector.Polar
    ): Vector.Polar {
        val location = location + velocity // Turning happens after movement

        val xWallAngle: Angle
        val xWallDistance: Double
        if (2 * location.x > battleField.width) {
            // RIGHT
            xWallAngle = ANGLE_RIGHT
            xWallDistance = (battleField.width - location.x - WALL_TANK_BUFFER).coerceAtLeast(0.0)
        } else {
            // LEFT
            xWallAngle = ANGLE_LEFT
            xWallDistance = (location.x - WALL_TANK_BUFFER).coerceAtLeast(0.0)
        }

        val yWallDistance: Double
        val yWallAngle: Angle
        if (2 * location.y > battleField.height) {
            // TOP
            yWallAngle = if (xWallAngle > Angle.HALF_CIRCLE) Angle.CIRCLE else Angle.ZERO
            yWallDistance = (battleField.height - location.y - WALL_TANK_BUFFER).coerceAtLeast(0.0)
        } else {
            // BOTTOM
            yWallAngle = ANGLE_DOWN
            yWallDistance = (location.y - WALL_TANK_BUFFER).coerceAtLeast(0.0)
        }

        val heading = velocity.theta + if (velocity.r < 0.0) Angle.HALF_CIRCLE else Angle.ZERO
        val xWallStick = TANK_MAX_RADIUS * (1 + abs(sin(heading - xWallAngle)))
        val yWallStick = TANK_MAX_RADIUS * (1 + abs(sin(heading - yWallAngle)))

        val xSmoothWall = xWallDistance < xWallStick
        val ySmoothWall = yWallDistance < yWallStick

        Debug.onDraw(DebugKey.WallSmoothMovement) {
            color = Color.BLUE
            drawLine(location, Polar(heading, velocity.r * 10))
            color = Color.YELLOW
            drawCircle(location + Polar(xWallAngle, xWallDistance), radius = 4.0)
            drawCircle(location + Polar(yWallAngle, yWallDistance), radius = 4.0)
            color = if (xSmoothWall) Color.RED else Color.GREEN
            drawProbe(location, Polar(xWallAngle, xWallStick), diameter = 8)
            color = if (ySmoothWall) Color.RED else Color.GREEN
            drawProbe(location, Polar(yWallAngle, yWallStick), diameter = 8)
        }

        val leftAngle: Angle
        val rightAngle: Angle
        if (xSmoothWall && ySmoothWall) {
            val xOpposite = (TANK_MAX_RADIUS - xWallDistance)
            val xDangerBearing = asin(xOpposite / TANK_MAX_RADIUS)

            val yOpposite = (TANK_MAX_RADIUS - yWallDistance)
            val yDangerBearing = asin(yOpposite / TANK_MAX_RADIUS)

            if (xWallAngle < yWallAngle) {
                // x wall is to the left
                // y wall is to the right
                leftAngle = xWallAngle - xDangerBearing
                rightAngle = yWallAngle + yDangerBearing
            } else {
                // x wall is to the right
                // y wall is to the left
                leftAngle = yWallAngle - yDangerBearing
                rightAngle = xWallAngle + xDangerBearing
            }
        } else if (xSmoothWall && xWallDistance < TANK_MAX_RADIUS) {
            val xOpposite = (TANK_MAX_RADIUS - xWallDistance)
            val xDangerBearing = asin(xOpposite / TANK_MAX_RADIUS)

            leftAngle = xWallAngle - xDangerBearing
            rightAngle = xWallAngle + xDangerBearing
        } else if (ySmoothWall && yWallDistance < TANK_MAX_RADIUS) {
            val yOpposite = (TANK_MAX_RADIUS - yWallDistance)
            val yDangerBearing = asin(yOpposite / TANK_MAX_RADIUS)

            leftAngle = yWallAngle - yDangerBearing
            rightAngle = yWallAngle + yDangerBearing
        } else {
            return movement
        }

        Debug.onDraw(DebugKey.WallSmoothMovement) {
            color = Color.YELLOW
            val length = maxOf(xWallStick, yWallStick)
            drawProbe(location, Polar(leftAngle, length))
            drawProbe(location, Polar(rightAngle, length))
        }

        val range = AngleRange(leftAngle, rightAngle)
        return if (heading + movement.theta !in range) movement
        else Vector.Polar(minBearing(heading, leftAngle, rightAngle), movement.r)
    }
}

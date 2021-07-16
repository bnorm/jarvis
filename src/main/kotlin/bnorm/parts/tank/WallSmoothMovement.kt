package bnorm.parts.tank

import bnorm.Vector
import bnorm.geo.AngleRange
import bnorm.parts.BattleField
import robocode.Rules
import robocode.util.Utils
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.sin
import kotlin.math.tan

class WallSmoothMovement(
    private val battleField: BattleField,
    private val movement: Movement,
) : Movement {
    companion object {
        private fun radius(speed: Double): Double {
            val turn = Rules.getTurnRateRadians(TANK_MAX_SPEED)
            return (speed / 2) / tan(turn / 2)
        }

        val TANK_MAX_RADIUS = radius(TANK_MAX_SPEED)
        const val WALL_TANK_BUFFER = TANK_SIZE / 2 + 8
    }

    override suspend fun invoke(location: Vector.Cartesian, velocity: Vector.Polar): Vector.Polar {
        return smooth(location, velocity, movement.invoke(location, velocity))
    }

    private fun smooth(
        location: Vector.Cartesian,
        velocity: Vector.Polar,
        movement: Vector.Polar
    ): Vector.Polar {
        val xWallAngle: Double
        val xWallDistance: Double
        if (2 * location.x > battleField.width) {
            // RIGHT
            xWallAngle = PI / 2
            xWallDistance = (battleField.width - location.x - WALL_TANK_BUFFER).coerceAtLeast(0.0)
        } else {
            // LEFT
            xWallAngle = 3 * PI / 2
            xWallDistance = (location.x - WALL_TANK_BUFFER).coerceAtLeast(0.0)
        }

        val yWallDistance: Double
        val yWallAngle: Double
        if (2 * location.y > battleField.height) {
            // TOP
            yWallAngle = if (xWallAngle > PI) 2 * PI else 0.0
            yWallDistance = (battleField.height - location.y - WALL_TANK_BUFFER).coerceAtLeast(0.0)
        } else {
            // BOTTOM
            yWallAngle = PI
            yWallDistance = (location.y - WALL_TANK_BUFFER).coerceAtLeast(0.0)
        }

        val heading = velocity.theta + if (velocity.r < 0.0) PI else 0.0
        val xWallStick = TANK_MAX_RADIUS * (1 + abs(sin(heading - xWallAngle)))
        val yWallStick = TANK_MAX_RADIUS * (1 + abs(sin(heading - yWallAngle)))

        val xSmoothWall = xWallDistance < xWallStick
        val ySmoothWall = yWallDistance < yWallStick

        val leftAngle: Double
        val rightAngle: Double
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
        val range = AngleRange(leftAngle, rightAngle)

        if (heading + movement.theta in range) {
            val leftBearing = Utils.normalRelativeAngle(leftAngle - heading)
            val rightBearing = Utils.normalRelativeAngle(rightAngle - heading)
            return if (abs(leftBearing) < abs(rightBearing)) {
                Vector.Polar(leftBearing, movement.r)
            } else {
                Vector.Polar(rightBearing, movement.r)
            }
        }
        return movement
    }
}

package bnorm.parts.tank

import bnorm.Vector
import bnorm.parts.BattleField
import bnorm.signMul
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

        val TANK_MAX_SPEED_RADIUS = radius(TANK_MAX_SPEED)
        const val SMOOTH_WALL_TANK_BUFFER = TANK_SIZE / 2 + 1
    }

    override suspend fun invoke(location: Vector.Cartesian, velocity: Vector.Polar): Vector.Polar {
        return smooth(location, velocity, movement.invoke(location, velocity))
    }

    private fun smooth(
        location: Vector.Cartesian,
        velocity: Vector.Polar,
        movement: Vector.Polar
    ): Vector.Polar {
        // TODO will movement turn into the danger zone?

        // Get movement heading
        val heading = velocity.theta + if (velocity.r < 0) PI else 0.0

        val xWallBearing: Double
        val xWallDistance: Double
        if (2 * location.x > battleField.width) {
            // RIGHT
            xWallBearing = Utils.normalRelativeAngle(heading - PI / 2)
            xWallDistance = battleField.width - location.x
        } else {
            // LEFT
            xWallBearing = Utils.normalRelativeAngle(heading - 3 * PI / 2)
            xWallDistance = location.x
        }

        val yWallDistance: Double
        val yWallBearing: Double
        if (2 * location.y > battleField.height) {
            // TOP
            yWallBearing = Utils.normalRelativeAngle(heading)
            yWallDistance = battleField.height - location.y
        } else {
            // BOTTOM
            yWallBearing = Utils.normalRelativeAngle(heading - PI)
            yWallDistance = location.y
        }

        // If bearing is negative, need to turn left
        // If bearing is positive, need to turn right

        val xWallStick = (1 + abs(sin(xWallBearing))) * TANK_MAX_SPEED_RADIUS + SMOOTH_WALL_TANK_BUFFER
        val yWallStick = (1 + abs(sin(yWallBearing))) * TANK_MAX_SPEED_RADIUS + SMOOTH_WALL_TANK_BUFFER

        val smoothXWall = abs(xWallBearing) <= PI / 2 && xWallDistance < xWallStick
        val smoothYWall = abs(yWallBearing) <= PI / 2 && yWallDistance < yWallStick

        return if (smoothXWall) {
            if (smoothYWall) {
                // Smooth X & Y wall
                // TODO over correct and let velocity coerce fix it
                val turnRate = 2 * Rules.getTurnRateRadians(velocity.r)

                if (xWallStick - xWallDistance > yWallStick - yWallDistance) {
                    Vector.Polar(signMul(xWallBearing) * turnRate, movement.r)
                } else {
                    Vector.Polar(signMul(yWallBearing) * turnRate, movement.r)
                }
            } else {
                // Smooth X wall
                // use x wall bearing to determine direction
                val opposite = (TANK_MAX_SPEED_RADIUS + SMOOTH_WALL_TANK_BUFFER - xWallDistance)
                    .coerceAtMost(TANK_MAX_SPEED_RADIUS)
                val turn = asin(opposite / TANK_MAX_SPEED_RADIUS) - abs(xWallBearing)
                if (turn > 0) {
                    Vector.Polar(signMul(xWallBearing) * turn, movement.r)
                } else {
                    movement
                }
            }
        } else if (smoothYWall) {
            // Smooth Y wall
            // use y wall bearing to determine direction
            val opposite = (TANK_MAX_SPEED_RADIUS + SMOOTH_WALL_TANK_BUFFER - yWallDistance)
                .coerceAtMost(TANK_MAX_SPEED_RADIUS)
            val turn = asin(opposite / TANK_MAX_SPEED_RADIUS) - abs(yWallBearing)
            if (turn > 0) {
                Vector.Polar(signMul(yWallBearing) * turn, movement.r)
            } else {
                movement
            }
        } else {
            movement
        }
    }
}

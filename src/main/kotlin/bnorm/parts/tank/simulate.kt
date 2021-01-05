package bnorm.parts.tank

import bnorm.Polar
import bnorm.Vector
import bnorm.parts.BattleField
import bnorm.robot.Robot
import bnorm.signMul
import bnorm.theta
import robocode.Rules
import robocode.util.Utils
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.tan

fun Robot.orbit(target: Robot, radius: Double) = simulate { location, velocity ->
    val heading = velocity.theta
    val newSpeed = (velocity.r + signMul(velocity.r)).coerceIn(-TANK_MAX_SPEED, TANK_MAX_SPEED)

    val desiredBearing = PI / 2 - atan((abs(velocity.r) / 2) / radius)
    val bearing = location.theta(target.latest.location)

    val clockwise = bearing - desiredBearing
    val counter = bearing + desiredBearing

    fun closest(heading: Double, h1: Double, h2: Double): Double {
        return if (abs(Utils.normalRelativeAngle(h1 - heading)) < abs(Utils.normalRelativeAngle(h2 - heading))) {
            h1
        } else {
            h2
        }
    }

    if (newSpeed < 0) {
        Polar(closest(heading, clockwise, counter), newSpeed)
    } else {
        Polar(closest(heading, clockwise + PI, counter + PI), newSpeed)
    }
}

fun Robot.simulate(
    nextFunction: (location: Vector.Cartesian, velocity: Vector.Polar) -> Vector.Polar,
) = battleField.simulate(latest.location, latest.velocity, nextFunction)

fun BattleField.simulate(
    location: Vector.Cartesian,
    velocity: Vector.Polar,
    nextFunction: (location: Vector.Cartesian, velocity: Vector.Polar) -> Vector.Polar,
): Sequence<Vector.Cartesian> {
    val battleField = this
    return sequence {
        var location = location
        var velocity = velocity
        while (true) {
            val v = nextFunction(location, velocity)
                .coerceVelocity(velocity)
                .smooth(battleField, location, velocity)
                .coerceVelocity(velocity)
            location += v
            yield(location)
            velocity = v
        }
    }
}

fun Vector.Polar.coerceVelocity(prevVelocity: Vector.Polar): Vector.Polar {
    val bearing = Utils.normalRelativeAngle(theta - prevVelocity.theta)
    val turnRate = Rules.getTurnRateRadians(prevVelocity.r)
    return if (abs(bearing) <= turnRate) this
    else Polar(prevVelocity.theta + sign(bearing) * turnRate, r)
}

private fun radius(speed: Double): Double {
    val turn = Rules.getTurnRateRadians(TANK_MAX_SPEED)
    return (speed / 2) / tan(turn / 2)
}

val TANK_MAX_SPEED_RADIUS = radius(TANK_MAX_SPEED)
const val SMOOTH_WALL_TANK_BUFFER = TANK_SIZE / 2 + 1

fun Vector.Polar.smooth(
    battleField: BattleField,
    location: Vector.Cartesian,
    prevVelocity: Vector.Polar
): Vector.Polar {
    // Get movement heading
    val heading = theta + if (r < 0) PI else 0.0

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
            val turnRate = 2 * Rules.getTurnRateRadians(prevVelocity.r)

            if (xWallStick - xWallDistance > yWallStick - yWallDistance) {
                Vector.Polar(theta + signMul(xWallBearing) * turnRate, r)
            } else {
                Vector.Polar(theta + signMul(yWallBearing) * turnRate, r)
            }
        } else {
            // Smooth X wall
            // use x wall bearing to determine direction
            val opposite = (TANK_MAX_SPEED_RADIUS + SMOOTH_WALL_TANK_BUFFER - xWallDistance)
                .coerceAtMost(TANK_MAX_SPEED_RADIUS)
            val turn = asin(opposite / TANK_MAX_SPEED_RADIUS) - abs(xWallBearing)
            if (turn > 0) {
                Vector.Polar(theta + signMul(xWallBearing) * turn, r)
            } else {
                this
            }
        }
    } else if (smoothYWall) {
        // Smooth Y wall
        // use y wall bearing to determine direction
        val opposite = (TANK_MAX_SPEED_RADIUS + SMOOTH_WALL_TANK_BUFFER - yWallDistance)
            .coerceAtMost(TANK_MAX_SPEED_RADIUS)
        val turn = asin(opposite / TANK_MAX_SPEED_RADIUS) - abs(yWallBearing)
        if (turn > 0) {
            Vector.Polar(theta + signMul(yWallBearing) * turn, r)
        } else {
            this
        }
    } else {
        this
    }
}

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

fun Robot.orbit(target: Robot, radius: Double, distance: Double) = simulate { location, velocity ->
    val heading = velocity.theta
    val theta = location.theta(target.latest.location)
    val bearingOffset = PI / 2 - atan((abs(velocity.r) / 2) / radius)

    val clockwise = theta - bearingOffset
    val counter = theta + bearingOffset

    fun closestBearing(heading: Double, h1: Double, h2: Double): Double {
        val b1 = Utils.normalRelativeAngle(h1 - heading)
        val b2 = Utils.normalRelativeAngle(h2 - heading)
        return if (abs(b1) < abs(b2)) b1 else b2
    }

    if (sign(distance) < 0) {
        Polar(closestBearing(heading + PI, clockwise, counter), distance)
    } else {
        Polar(closestBearing(heading, clockwise, counter), distance)
    }
}

fun Robot.simulate(
    movementFunction: (location: Vector.Cartesian, velocity: Vector.Polar) -> Vector.Polar,
) = battleField.simulate(latest.location, latest.velocity, movementFunction)

fun BattleField.simulate(
    location: Vector.Cartesian,
    velocity: Vector.Polar,
    movementFunction: (location: Vector.Cartesian, velocity: Vector.Polar) -> Vector.Polar,
): Sequence<Vector.Cartesian> {
    return sequence {
        var location = location
        var velocity = velocity
        while (true) {
            val move = smooth(this@simulate, location, velocity, movementFunction(location, velocity))
            val v = simulateVelocity(velocity, move.theta, move.r)
            location += v
            yield(location)
            velocity = v
        }
    }
}

fun simulateVelocity(velocity: Vector.Polar, turn: Double, distance: Double): Vector.Polar =
    Vector.Polar(velocity.theta + simulateTurn(velocity.r, turn), simulateSpeed(velocity.r, distance))

fun simulateTurn(currentSpeed: Double, turn: Double): Double =
    if (turn < 0.0) -simulateTurn(currentSpeed, -turn)
    else turn.coerceAtMost(Rules.getTurnRateRadians(currentSpeed))

fun simulateSpeed(currentSpeed: Double, distance: Double): Double {
    if (distance < 0.0) {
        // Flip function so distance is always positive
        return -simulateSpeed(-currentSpeed, -distance)
    }

    if (currentSpeed < 0.0) {
        // decelerating
        val newSpeed = currentSpeed + TANK_DECELERATION
        if (newSpeed > 0.0) {
            // flipped from decelerating to accelerating
            val decelerationTime = -currentSpeed / TANK_DECELERATION
            val accelerationTime = 1.0 - decelerationTime
            return (TANK_DECELERATION * decelerationTime * decelerationTime +
                    TANK_ACCELERATION * accelerationTime * accelerationTime)
                .coerceAtMost(distance).coerceAtMost(TANK_MAX_SPEED)
        }
        return newSpeed
    } else {
        val decelerationTime = currentSpeed / TANK_DECELERATION
        val decelerationDistance = 0.5 * TANK_DECELERATION * decelerationTime * decelerationTime + decelerationTime
        if (distance <= decelerationDistance) {
            // start decelerating so velocity is 0 when distance is 0
            val time = decelerationDistance / (decelerationTime + 1)
            if (time <= 1) {
                return (currentSpeed - TANK_DECELERATION).coerceAtMost(distance)
            } else {
                val newSpeed = time * TANK_DECELERATION
                if (currentSpeed < newSpeed) {
                    return currentSpeed
                } else if (currentSpeed - newSpeed > TANK_DECELERATION) {
                    return currentSpeed - TANK_DECELERATION
                } else {
                    return newSpeed
                }
            }
        } else {
            // accelerating
            return (currentSpeed + TANK_ACCELERATION).coerceAtMost(TANK_MAX_SPEED)
        }
    }
}

fun Vector.Polar.coerceTurn(prevVelocity: Vector.Polar): Vector.Polar {
    val turn = Utils.normalRelativeAngle(theta - prevVelocity.theta)
    return Polar(prevVelocity.theta + simulateTurn(prevVelocity.r, turn), r)
}

private fun radius(speed: Double): Double {
    val turn = Rules.getTurnRateRadians(TANK_MAX_SPEED)
    return (speed / 2) / tan(turn / 2)
}

val TANK_MAX_SPEED_RADIUS = radius(TANK_MAX_SPEED)
const val SMOOTH_WALL_TANK_BUFFER = TANK_SIZE / 2 + 1

fun smooth(
    battleField: BattleField,
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

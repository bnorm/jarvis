package bnorm.parts.tank

import bnorm.Vector
import bnorm.robot.Robot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import robocode.Rules

fun Robot.simulate(movement: Movement) =
    simulate(latest.location, latest.velocity, movement)

fun simulate(
    location: Vector.Cartesian,
    velocity: Vector.Polar,
    movement: Movement,
): Flow<Vector.Cartesian> = flow {
    var location = location
    var velocity = velocity
    while (true) {
        val move = movement.invoke(location, velocity)
        val v = simulateVelocity(velocity, move.theta, move.r)
        location += v
        emit(location)
        velocity = v
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

package bnorm.parts.tank

import bnorm.Vector
import bnorm.geo.Angle
import bnorm.geo.Segment
import bnorm.geo.contains
import bnorm.geo.intersect
import bnorm.parts.BattleField
import bnorm.robot.Robot
import bnorm.sim.getTankTurnRate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun Robot.simulate(movement: Movement) =
    battleField.simulate(latest.location, latest.velocity, movement)

fun BattleField.simulate(
    location: Vector.Cartesian,
    velocity: Vector.Polar,
    movement: Movement,
): Flow<Vector.Cartesian> {
    // https://robowiki.net/wiki/Robocode/Game_Physics#Robocode_processing_loop

    val battleField = this
    return flow {
        var location = location
        var velocity = velocity
        while (true) {
            val move = movement.invoke(location, velocity)
            val v = simulateVelocity(velocity, move.theta, move.r)
            val next = location + v
            location = if (next !in battleField.movable) {
                battleField.movable.intersect(Segment(location, next)).singleOrNull()
                    ?: Vector.Cartesian(
                        next.x.coerceIn(battleField.movable.xRange),
                        next.y.coerceIn(battleField.movable.yRange),
                    )
            } else {
                next
            }
            emit(location)
            velocity = v
        }
    }
}

fun simulateVelocity(velocity: Vector.Polar, turn: Angle, distance: Double): Vector.Polar =
    Vector.Polar(velocity.theta + simulateTurn(velocity.r, turn), simulateSpeed(velocity.r, distance))

fun simulateTurn(currentSpeed: Double, turn: Angle): Angle =
    if (turn < Angle.ZERO) -simulateTurn(currentSpeed, -turn)
    else turn.coerceAtMost(getTankTurnRate(currentSpeed))

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
            val time = decelerationDistance / (decelerationTime + 1) - 1
            if (time <= 1) {
                return (currentSpeed - TANK_DECELERATION).coerceIn(0.0, distance)
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

package bnorm.robot

import bnorm.parts.gun.GuessFactorSnapshot
import bnorm.parts.tank.TANK_ACCELERATION
import bnorm.parts.tank.TANK_DECELERATION
import bnorm.parts.tank.TANK_MAX_SPEED
import bnorm.r
import bnorm.sqr
import bnorm.theta
import robocode.util.Utils
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

data class RobotSnapshot(
    val scan: RobotScan,
    val rotateDirection: Int,
    val moveDirection: Int,
    val distance: Double,
    val lateralSpeed: Double,
    val advancingSpeed: Double,
    val acceleration: Double,
    val distLast10: Double,
    val distLast30: Double,
    val distLast90: Double,
    val timeSinceMovement: Long,
    val timeSinceReverse: Long,
    val timeSinceDeceleration: Long,
    val timeSinceBullet: Long,
    val wallDistance: Double,
    val forwardWallDistance: Double,
    val backwardWallDistance: Double,
    val cornerDistance: Double,
    val cornerDirection: Double,
) : GuessFactorSnapshot {
    val nTime: Double get() = normalize(scan.time)
    val nTimeSinceMovement: Double get() = normalize(timeSinceMovement)
    val nTimeSinceReverse: Double get() = normalize(timeSinceReverse)
    val nTimeSinceDeceleration: Double get() = normalize(timeSinceDeceleration)
    val nTimeSinceBullet: Double get() = normalize(timeSinceBullet)

    companion object {
        val DIMENSIONS = listOf(
            RobotSnapshot::distance,
            RobotSnapshot::lateralSpeed,
            RobotSnapshot::advancingSpeed,
            RobotSnapshot::acceleration,
            RobotSnapshot::distLast10,
            RobotSnapshot::distLast30,
            RobotSnapshot::distLast90,
            RobotSnapshot::nTime,
            RobotSnapshot::nTimeSinceMovement,
            RobotSnapshot::nTimeSinceReverse,
            RobotSnapshot::nTimeSinceDeceleration,
//            RobotSnapshot::nTimeSinceBullet,
//            RobotSnapshot::wallDistance,
            RobotSnapshot::forwardWallDistance,
            RobotSnapshot::backwardWallDistance,
//            RobotSnapshot::cornerDistance,
//            RobotSnapshot::cornerDirection,
        )
    }

    val guessFactorDimensions: DoubleArray = DoubleArray(DIMENSIONS.size) {
        DIMENSIONS[it].invoke(this@RobotSnapshot)
    }

    var prev: RobotSnapshot? = null
    override var guessFactor: Double = 0.0
}

fun RobotSnapshot.history() = generateSequence(this) { curr -> curr.prev }

fun RobotSnapshot?.traveledOver(turns: Long): Double {
    if (this == null) return 0.0
    val end = scan.location
    val time = scan.time

    val start = history().takeWhile { time - it.scan.time < turns }.last().scan.location
    return start.r(end)
}

fun RobotSnapshot?.timeSinceMovement(speed: Double): Long {
    if (this == null) return 0
    if (speed != 0.0) return this.timeSinceMovement + 1
    return 0
}

fun RobotSnapshot?.timeSinceReverse(direction: Int): Long {
    if (this == null) return 0
    if (this.moveDirection == direction) return this.timeSinceReverse + 1
    return 0
}

fun RobotSnapshot?.timeSinceDeceleration(acceleration: Double): Long {
    if (this == null) return 0
    if (acceleration >= 0.0) return this.timeSinceDeceleration + 1
    return 0
}

fun RobotSnapshot?.timeSinceBullet(robot: Robot): Long {
    if (this == null) return 0
    val current = robot.latest.energy
    val previous = robot.latest.prev?.energy ?: current
    if (current < previous) return 0
    return this.timeSinceDeceleration + 1
}

fun normalize(value: Long): Double = 1.0 - 1.0 / (1.0 + value)
fun normalize(min: Double, value: Double, max: Double): Double = (value - min) / (max - min)

fun RobotService.robotSnapshot(
    scan: RobotScan,
    prevSnapshot: RobotSnapshot?
): RobotSnapshot {
    val selfScan = self.latest
    val battleField = self.battleField

    val theta = theta(selfScan.location, scan.location)
    val distance = r(selfScan.location, scan.location)

    val speed = scan.velocity.r
    val heading = Utils.normalAbsoluteAngle(scan.velocity.theta)

    val relativeBearing = heading - theta

    val lateralSpeed = sin(relativeBearing) * speed
    val advancingSpeed = cos(relativeBearing) * speed

    val rotateDirection = when (val d = sign(lateralSpeed).roundToInt()) {
        0 -> prevSnapshot?.rotateDirection ?: 0
        else -> d
    }
    val moveDirection = when (val d = sign(speed).roundToInt()) {
        0 -> prevSnapshot?.moveDirection ?: 0
        else -> d
    }

    val prevVelocity = (scan.prev?.takeIf { it.time == selfScan.time - 1 } ?: scan).velocity
    val acceleration = (prevSnapshot?.moveDirection ?: moveDirection) * (speed - prevVelocity.r)

    val distLast10 = prevSnapshot.traveledOver(10)
    val distLast30 = prevSnapshot.traveledOver(30)
    val distLast90 = prevSnapshot.traveledOver(90)

    val timeSinceMovement = prevSnapshot.timeSinceMovement(speed)
    val timeSinceReverse = prevSnapshot.timeSinceReverse(moveDirection)
    val timeSinceDeceleration = prevSnapshot.timeSinceDeceleration(acceleration)
    val timeSinceBullet = prevSnapshot.timeSinceBullet(self)

    val x = scan.location.x
    val y = scan.location.y
    val xInverse = battleField.width - x
    val yInverse = battleField.height - y

    val wallDistance = minOf(x, y, xInverse, yInverse)
    val cornerDistance = sqrt(sqr(minOf(x, xInverse)) + sqr(minOf(y, yInverse)))
    val cornerDirection = sign(if (prevSnapshot != null) cornerDistance - prevSnapshot.cornerDistance else 0.0)

    val headingXWallDistance = (if (heading < PI) xInverse else x) / abs(cos(PI / 2 - heading))
    val reverseXWallDistance = (if (heading < PI) x else xInverse) / abs(cos(PI / 2 - heading))
    val headingYWallDistance = (if (heading in PI / 2..PI * 3 / 4) y else yInverse) / abs(cos(heading))
    val reverseYWallDistance = (if (heading in PI / 2..PI * 3 / 4) yInverse else y) / abs(cos(heading))
    val forwardWallDistance: Double
    val backwardWallDistance: Double
    if (moveDirection < 0) {
        forwardWallDistance = minOf(reverseXWallDistance, reverseYWallDistance)
        backwardWallDistance = minOf(headingXWallDistance, headingYWallDistance)
    } else {
        forwardWallDistance = minOf(headingXWallDistance, headingYWallDistance)
        backwardWallDistance = minOf(reverseXWallDistance, reverseYWallDistance)
    }

    return RobotSnapshot(
        scan = scan,
        rotateDirection = rotateDirection,
        moveDirection = moveDirection,
        distance = normalize(0.0, distance, battleField.diagonal),
        lateralSpeed = normalize(0.0, abs(lateralSpeed), TANK_MAX_SPEED),
        advancingSpeed = normalize(-TANK_MAX_SPEED, advancingSpeed, TANK_MAX_SPEED),
        acceleration = normalize(-TANK_DECELERATION, acceleration, TANK_ACCELERATION),
        distLast10 = normalize(0.0, distLast10, 10 * TANK_MAX_SPEED),
        distLast30 = normalize(0.0, distLast30, 30 * TANK_MAX_SPEED),
        distLast90 = normalize(0.0, distLast90, 90 * TANK_MAX_SPEED),
        timeSinceMovement = timeSinceMovement,
        timeSinceReverse = timeSinceReverse,
        timeSinceDeceleration = timeSinceDeceleration,
        timeSinceBullet = timeSinceBullet,
        wallDistance = normalize(0.0, wallDistance, minOf(battleField.width / 2, battleField.height / 2)),
        forwardWallDistance = normalize(0.0, forwardWallDistance, battleField.diagonal),
        backwardWallDistance = normalize(0.0, backwardWallDistance, battleField.diagonal),
        cornerDistance = cornerDistance,
        cornerDirection = cornerDirection
    )
}

package bnorm.robot

import bnorm.Vector
import bnorm.normalize
import bnorm.parts.gun.GuessFactorSnapshot
import bnorm.parts.tank.TANK_ACCELERATION
import bnorm.parts.tank.TANK_DECELERATION
import bnorm.parts.tank.TANK_MAX_SPEED
import bnorm.r
import bnorm.r2
import bnorm.signMul
import bnorm.sqr
import bnorm.theta
import robocode.util.Utils
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

data class RobotSnapshot(
    val scan: RobotScan,
    val rotateDirection: Int,
    val moveDirection: Int,
    val distance: Double,
    val traveled: Double,
    val lateralSpeed: Double,
    val advancingSpeed: Double,
    val acceleration: Double,
    val distLast10: Double,
    val distLast50: Double,
    val distLast100: Double,
    val timeSinceReverse: Double,
    val timeSinceDeceleration: Double,
    val wallDistance: Double,
    val forwardWallDistance: Double,
    val backwardWallDistance: Double,
) : GuessFactorSnapshot {
    companion object {
        val DIMENSIONS = listOf(
            RobotSnapshot::distance,
            RobotSnapshot::lateralSpeed,
            RobotSnapshot::advancingSpeed,
            RobotSnapshot::acceleration,
            RobotSnapshot::distLast10,
            RobotSnapshot::distLast50,
            RobotSnapshot::distLast100,
            RobotSnapshot::timeSinceReverse,
            RobotSnapshot::timeSinceDeceleration,
            RobotSnapshot::wallDistance,
            RobotSnapshot::forwardWallDistance,
            RobotSnapshot::backwardWallDistance,
        )
    }

    val dimensions: DoubleArray = DoubleArray(DIMENSIONS.size) {
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
    return start.r2(end)
}

fun RobotSnapshot?.timeSinceReverse(direction: Int): Double {
    if (this == null) return 0.0
    if (this.rotateDirection == direction) return this.timeSinceReverse + 1.0
    return 0.0
}

fun RobotSnapshot?.timeSinceDeceleration(acceleration: Double): Double {
    if (this == null) return 0.0
    if (acceleration >= 0.0) return this.timeSinceDeceleration + 1.0
    return 0.0
}

fun RobotService.robotSnapshot(
    currScan: RobotScan,
    prevSnapshot: RobotSnapshot?,
    prevScan: RobotScan?
): RobotSnapshot {
    val selfScan = self.latest
    val battleField = self.battleField

    val theta = theta(selfScan.location, currScan.location)
    val distance = r(selfScan.location, currScan.location)

    val speed = currScan.velocity.r
    val heading = Utils.normalAbsoluteAngle(currScan.velocity.theta)

    val relativeBearing = heading - theta

    val lateralSpeed = sin(relativeBearing) * speed
    val advancingSpeed = cos(relativeBearing) * speed

    val prevVelocity: Vector.Polar
    val traveled: Double
    if (prevScan != null) {
        prevVelocity =
            prevScan.velocity.takeIf { prevScan.time == selfScan.time - 1 } ?: currScan.velocity
        traveled = r(prevScan.location, currScan.location) / (currScan.time - prevScan.time)
    } else {
        prevVelocity = currScan.velocity
        traveled = 0.0
    }

    val acceleration = signMul(prevVelocity.r) * (speed - prevVelocity.r)

    val rotateDirection = when (val d = sign(lateralSpeed).roundToInt()) {
        0 -> prevSnapshot?.rotateDirection ?: 0
        else -> d
    }
    val moveDirection = when (val d = sign(speed).roundToInt()) {
        0 -> prevSnapshot?.moveDirection ?: 0
        else -> d
    }

    val distLast10 = prevSnapshot.traveledOver(10)
    val distLast50 = prevSnapshot.traveledOver(50)
    val distLast100 = prevSnapshot.traveledOver(100)

    val timeSinceReverse = prevSnapshot.timeSinceReverse(rotateDirection)
    val timeSinceDeceleration = prevSnapshot.timeSinceDeceleration(acceleration)

    val x = currScan.location.x
    val y = currScan.location.y
    val xInverse = battleField.width - x
    val yInverse = battleField.height - y

    val wallDistance = minOf(x, y, xInverse, yInverse)

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
        scan = currScan,
        rotateDirection = rotateDirection,
        moveDirection = moveDirection,
        distance = normalize(0.0, distance, battleField.diagonal),
        lateralSpeed = normalize(0.0, abs(lateralSpeed), TANK_MAX_SPEED),
        advancingSpeed = normalize(-TANK_MAX_SPEED, advancingSpeed, TANK_MAX_SPEED),
        acceleration = normalize(-TANK_DECELERATION, acceleration, TANK_ACCELERATION),
        distLast10 = normalize(0.0, distLast10, sqr(10 * TANK_MAX_SPEED)),
        distLast50 = normalize(0.0, distLast50, sqr(50 * TANK_MAX_SPEED)),
        distLast100 = normalize(0.0, distLast100, sqr(100 * TANK_MAX_SPEED)),
        timeSinceReverse = 1.0 - 1.0 / (1.0 + timeSinceReverse),
        timeSinceDeceleration = 1.0 - 1.0 / (1.0 + timeSinceDeceleration),
        traveled = normalize(0.0, traveled, TANK_MAX_SPEED),
        wallDistance = normalize(0.0, wallDistance, minOf(battleField.width / 2, battleField.height / 2)),
        forwardWallDistance = normalize(0.0, forwardWallDistance, battleField.diagonal),
        backwardWallDistance = normalize(0.0, backwardWallDistance, battleField.diagonal),
    )
}

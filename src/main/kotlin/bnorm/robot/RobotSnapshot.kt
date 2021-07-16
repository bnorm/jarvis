package bnorm.robot

import bnorm.parts.gun.GuessFactorSnapshot
import bnorm.parts.tank.TANK_ACCELERATION
import bnorm.parts.tank.TANK_DECELERATION
import bnorm.parts.tank.TANK_MAX_SPEED
import bnorm.r
import bnorm.robot.snapshot.WallProbe
import bnorm.sqr
import bnorm.theta
import bnorm.truncate
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import robocode.util.Utils
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
data class RobotSnapshot(
    val scan: RobotScan,
    val moveDirection: Int,
    val rotateDirection: Int,
    val accelerationDirection: Int,
    val wallProbe: WallProbe,
    val distance: Double,
    val lateralSpeed: Double,
    val advancingSpeed: Double,
    val acceleration: Double,
    val lateralAcceleration: Double,
    val advancingAcceleration: Double,
    val distLast10: Double,
    val distLast30: Double,
    val distLast90: Double,
    val timeDeltaMoveDirection: Long,
    val timeDeltaRotateDirection: Long,
    val timeDeltaAccelerationDirection: Long,
    val timeDeltaVelocityChange: Long,
    val cornerDistance: Double,
    val cornerDirection: Double,
    val activeWaveCount: Long,
) : GuessFactorSnapshot {
    val gfDirection: Int get() = rotateDirection

    val nLateralAcceleration: Double get() = normalize(-TANK_DECELERATION, lateralAcceleration, TANK_ACCELERATION)
    val nAdvancingAcceleration: Double get() = normalize(-TANK_DECELERATION, advancingAcceleration, TANK_DECELERATION)
    val nAcceleration: Double get() = normalize(-TANK_DECELERATION, acceleration, TANK_ACCELERATION)

    val nTime: Double get() = normalize(scan.time)
    val nTimeDeltaMovement: Double get() = normalize(timeDeltaMoveDirection)
    val nTimeDeltaRotateDirection: Double get() = normalize(timeDeltaRotateDirection)
    val nTimeDeltaAccelerationDirection: Double get() = normalize(timeDeltaAccelerationDirection)
    val nTimeDeltaVelocityChange: Double get() = normalize(timeDeltaVelocityChange)
    val nActiveWaveCount: Double get() = normalize(activeWaveCount)

    val nForwardWallDistance: Double
        get() = normalize(0.0, wallProbe.perpendicular.forward, wallProbe.position.diagonal)
    val nBackwardWallDistance: Double
        get() = normalize(0.0, wallProbe.perpendicular.backward, wallProbe.position.diagonal)

    companion object {
        val DIMENSIONS = listOf(
            RobotSnapshot::distance,
            RobotSnapshot::lateralSpeed,
            RobotSnapshot::advancingSpeed,
//            RobotSnapshot::nLateralAcceleration,
//            RobotSnapshot::nAdvancingAcceleration,
            RobotSnapshot::nAcceleration,
            RobotSnapshot::distLast10,
            RobotSnapshot::distLast30,
            RobotSnapshot::distLast90,
//            RobotSnapshot::nTime,
            RobotSnapshot::nTimeDeltaMovement,
            RobotSnapshot::nTimeDeltaRotateDirection,
            RobotSnapshot::nTimeDeltaAccelerationDirection,
//            RobotSnapshot::nTimeDeltaVelocityChange,
            RobotSnapshot::nForwardWallDistance,
            RobotSnapshot::nBackwardWallDistance,
//            RobotSnapshot::cornerDistance,
//            RobotSnapshot::cornerDirection,
//            RobotSnapshot::nActiveWaveCount,
        )
    }

    @Transient
    val guessFactorDimensions: DoubleArray = DoubleArray(DIMENSIONS.size) {
        DIMENSIONS[it].invoke(this@RobotSnapshot)
    }

    @Transient
    var prev: RobotSnapshot? = null
    override var guessFactor: Double = Double.NaN
}

fun RobotSnapshot.history() = generateSequence(this) { curr -> curr.prev }

fun RobotSnapshot?.traveledOver(turns: Long): Double {
    if (this == null) return 0.0
    val end = scan.location
    val time = scan.time

    val start = history().takeWhile { time - it.scan.time < turns }.last().scan.location
    return start.r(end)
}

fun RobotSnapshot?.timeDeltaMoveDirection(movementDirection: Int): Long {
    return if (this != null && movementDirection == this.moveDirection)
        this.timeDeltaMoveDirection + 1
    else 0
}

fun RobotSnapshot?.timeDeltaRotateDirection(rotateDirection: Int): Long {
    return if (this != null && rotateDirection == this.rotateDirection)
        this.timeDeltaRotateDirection + 1
    else 0
}

fun RobotSnapshot?.timeDeltaAccelerationDirection(accelerationDirection: Int): Long {
    return if (this != null && accelerationDirection == this.accelerationDirection)
        this.timeDeltaAccelerationDirection + 1
    else 0
}

fun RobotSnapshot?.timeDeltaLateralSpeed(lateralSpeed: Double): Long {
    return if (this != null && lateralSpeed == this.lateralSpeed)
        this.timeDeltaVelocityChange + 1
    else 0
}

fun normalize(value: Long): Double = 1.0 - 1.0 / (1.0 + value / 10.0)
fun normalize(value: Double): Double = 1.0 - 1.0 / (1.0 + value / 10.0)
fun normalize(min: Double, value: Double, max: Double): Double = (value - min) / (max - min)

fun robotSnapshot(
    source: Robot,
    scan: RobotScan,
    prevSnapshot: RobotSnapshot?,
    activeWaveCount: Long = 0,
): RobotSnapshot {
    val selfScan = source.latest
    val battleField = source.battleField
    val prevScan = scan.prev?.takeIf { it.time == selfScan.time - 1 } ?: scan
    val prevVelocity = prevScan.velocity

    val theta = theta(selfScan.location, scan.location)
    val distance = r(selfScan.location, scan.location)

    val speed = scan.velocity.r
    val heading = Utils.normalAbsoluteAngle(scan.velocity.theta)

    val relativeBearing = heading - theta

    val lateralSpeed = sin(relativeBearing) * speed
    val advancingSpeed = cos(relativeBearing) * speed

    // Avoid really small movement changes
    val moveDirection = when (val d = sign(truncate(speed, 1e-3)).roundToInt()) {
        0 -> prevSnapshot?.moveDirection ?: 0
        else -> d
    }
    val acceleration = moveDirection * (speed - prevVelocity.r)

    // Avoid really small movement changes
    val rotateDirection = when (val d = sign(truncate(lateralSpeed, 1e-3)).roundToInt()) {
        0 -> prevSnapshot?.rotateDirection ?: 0
        else -> d
    }

    // Avoid really small movement changes
    val accelerationDirection = when (val d = sign(truncate(acceleration * lateralSpeed, 1e-3)).roundToInt()) {
        0 -> prevSnapshot?.accelerationDirection ?: 0
        else -> d
    }

    val lateralAcceleration = abs(sin(relativeBearing)) * acceleration
    val advancingAcceleration = cos(relativeBearing) * acceleration

    val distLast10 = prevSnapshot.traveledOver(10)
    val distLast30 = prevSnapshot.traveledOver(30)
    val distLast90 = prevSnapshot.traveledOver(90)

    val timeDeltaMoveDirection = prevSnapshot.timeDeltaMoveDirection(moveDirection)
    val timeDeltaRotateDirection = prevSnapshot.timeDeltaRotateDirection(accelerationDirection)
    val timeDeltaAccelerationDirection = prevSnapshot.timeDeltaAccelerationDirection(accelerationDirection)
    val timeDeltaVelocityChange = prevSnapshot.timeDeltaLateralSpeed(lateralSpeed)

    val wallProbe = WallProbe(
        battleField,
        scan.location,
        heading + if (moveDirection < 0.0) PI else 0.0,
        theta + rotateDirection * (PI / 2)
    )

    val west = scan.location.x
    val south = scan.location.y
    val east = battleField.width - west
    val north = battleField.height - south

    val cornerDistance = sqrt(sqr(minOf(west, east)) + sqr(minOf(south, north)))
    val cornerDirection = sign(if (prevSnapshot != null) cornerDistance - prevSnapshot.cornerDistance else 0.0)

    return RobotSnapshot(
        scan = scan,
        moveDirection = moveDirection,
        rotateDirection = rotateDirection,
        accelerationDirection = accelerationDirection,
        wallProbe = wallProbe,
        distance = normalize(0.0, distance, battleField.diagonal),
        lateralSpeed = normalize(0.0, abs(lateralSpeed), TANK_MAX_SPEED),
        advancingSpeed = normalize(-TANK_MAX_SPEED, advancingSpeed, TANK_MAX_SPEED),
        acceleration = acceleration,
        lateralAcceleration = lateralAcceleration,
        advancingAcceleration = advancingAcceleration,
        distLast10 = normalize(0.0, distLast10, 10 * TANK_MAX_SPEED),
        distLast30 = normalize(0.0, distLast30, 30 * TANK_MAX_SPEED),
        distLast90 = normalize(0.0, distLast90, 90 * TANK_MAX_SPEED),
        timeDeltaMoveDirection = timeDeltaMoveDirection,
        timeDeltaRotateDirection = timeDeltaRotateDirection,
        timeDeltaAccelerationDirection = timeDeltaAccelerationDirection,
        timeDeltaVelocityChange = timeDeltaVelocityChange,
        cornerDistance = cornerDistance,
        cornerDirection = cornerDirection,
        activeWaveCount = activeWaveCount,
    )
}

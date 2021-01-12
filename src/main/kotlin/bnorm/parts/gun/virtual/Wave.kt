package bnorm.parts.gun.virtual

import bnorm.Vector
import bnorm.parts.tank.Movement
import bnorm.parts.tank.OrbitMovement
import bnorm.parts.tank.TANK_MAX_SPEED
import bnorm.parts.tank.WallSmoothMovement
import bnorm.parts.tank.simulate
import bnorm.r
import bnorm.r2
import bnorm.robot.EscapeAngle
import bnorm.robot.Robot
import bnorm.robot.snapshot
import bnorm.sqr
import bnorm.theta
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import robocode.util.Utils
import kotlin.math.abs
import kotlin.math.asin

data class Wave(
    val origin: Vector.Cartesian,
    val speed: Double,
    val time: Long,
    val context: WaveContext,
)

fun Wave.radius(time: Long): Double {
    return speed * (time - this.time)
}

fun Wave.escapeAngle(speed: Double): Double {
    return asin(speed / this.speed)
}

suspend fun escapeAngle(self: Robot, robot: Robot, speed: Double): EscapeAngle {
//    val escapeAngle = asin(TANK_MAX_SPEED / speed)
//    return EscapeAngle(escapeAngle, escapeAngle)

    return coroutineScope {
        val targetLocation = robot.latest.location
        val sourceLocation = self.latest.location
        val theta = sourceLocation.theta(targetLocation)
        val distance = sourceLocation.r(targetLocation).coerceAtLeast(500.0)

        val forward = async {
            val movement =
                WallSmoothMovement(robot.battleField, OrbitMovement(self, distance, 1.0 * robot.snapshot.moveDirection))
            abs(Utils.normalRelativeAngle(robot.theta(movement, sourceLocation, speed) - theta))
        }

        val reverse = async {
            val movement =
                WallSmoothMovement(robot.battleField, OrbitMovement(self, distance, -1.0 * robot.snapshot.moveDirection))
            abs(Utils.normalRelativeAngle(robot.theta(movement, sourceLocation, speed) - theta))
        }

        EscapeAngle(forward.await(), reverse.await())
    }
}

private suspend fun Robot.theta(
    movement: Movement,
    source: Vector.Cartesian,
    speed: Double
): Double {
    var time = 0
    val location = simulate(movement).first { sqr(time++ * speed) >= source.r2(it) }
    return source.theta(location)
}

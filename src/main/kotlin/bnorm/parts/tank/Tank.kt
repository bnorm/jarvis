package bnorm.parts.tank

import bnorm.Vector
import bnorm.parts.RobotPart
import bnorm.r
import bnorm.theta
import robocode.AdvancedRobot
import robocode.Rules
import robocode.util.Utils
import kotlin.math.PI

const val TANK_MAX_SPEED = Rules.MAX_VELOCITY
const val TANK_ACCELERATION = Rules.ACCELERATION
const val TANK_DECELERATION = Rules.DECELERATION
const val TANK_SIZE = 36.0

interface Tank : RobotPart {
    val heading: Double
    val speed: Double

    fun setTurn(radians: Double)
    fun setAhead(distance: Double)
}

fun Tank.moveTo(destination: Vector) {
    var bearing = Utils.normalRelativeAngle(theta(x, y, destination.x, destination.y) - heading)
    var distance = r(x, y, destination.x, destination.y)

    // Is it better to go backwards?
    if (bearing > PI / 2) {
        bearing -= PI
        distance = -distance
    } else if (bearing < -PI / 2) {
        bearing += PI
        distance = -distance
    }

    setTurn(bearing)
    setAhead(distance)
}

fun Tank(robot: AdvancedRobot): Tank {
    return object : Tank, RobotPart by RobotPart(robot) {
        override val heading: Double get() = robot.headingRadians
        override val speed: Double get() = robot.velocity

        override fun setTurn(radians: Double) {
            robot.setTurnRightRadians(radians)
        }

        override fun setAhead(distance: Double) {
            robot.setAhead(distance)
        }
    }
}

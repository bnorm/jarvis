package bnorm.parts.tank

import bnorm.Polar
import bnorm.Vector
import bnorm.geo.Angle
import bnorm.geo.normalizeRelative
import bnorm.parts.RobotPart
import bnorm.r
import bnorm.theta
import robocode.AdvancedRobot
import robocode.Rules

const val TANK_MAX_SPEED = Rules.MAX_VELOCITY
const val TANK_ACCELERATION = Rules.ACCELERATION
const val TANK_DECELERATION = Rules.DECELERATION
const val TANK_SIZE = 36.0

interface Tank : RobotPart {
    val heading: Angle
    val speed: Double

    fun setTurn(angle: Angle)
    fun setAhead(distance: Double)
}

fun Tank.moveTo(destination: Vector) {
    var bearing = (theta(x, y, destination.x, destination.y) - heading).normalizeRelative()
    var distance = r(x, y, destination.x, destination.y)

    // Is it better to go backwards?
    if (bearing > Angle.QUARTER_CIRCLE) {
        bearing -= Angle.HALF_CIRCLE
        distance = -distance
    } else if (bearing < -Angle.QUARTER_CIRCLE) {
        bearing += Angle.HALF_CIRCLE
        distance = -distance
    }

    setTurn(bearing)
    setAhead(distance)
}

fun moveTo(location: Vector.Cartesian, velocity: Vector.Polar, destination: Vector): Vector.Polar {
    var bearing = (location.theta(destination) - velocity.theta).normalizeRelative()
    var distance = location.r(destination)

    // Is it better to go backwards?
    if (bearing > Angle.QUARTER_CIRCLE) {
        bearing -= Angle.HALF_CIRCLE
        distance = -distance
    } else if (bearing < -Angle.QUARTER_CIRCLE) {
        bearing += Angle.HALF_CIRCLE
        distance = -distance
    }

    return Polar(bearing, distance)
}

fun Tank(robot: AdvancedRobot): Tank {
    return object : Tank, RobotPart by RobotPart(robot) {
        override val heading: Angle get() = Angle(robot.headingRadians)
        override val speed: Double get() = robot.velocity

        override fun setTurn(angle: Angle) {
            robot.setTurnRightRadians(angle.radians)
        }

        override fun setAhead(distance: Double) {
            robot.setAhead(distance)
        }
    }
}

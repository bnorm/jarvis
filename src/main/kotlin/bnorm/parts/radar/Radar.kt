package bnorm.parts.radar

import bnorm.geo.Angle
import bnorm.parts.RobotPart
import robocode.AdvancedRobot

interface Radar : RobotPart {
    val heading: Angle

    fun setTurn(angle: Angle)
}

fun Radar(robot: AdvancedRobot): Radar {
    return object : Radar, RobotPart by RobotPart(robot) {
        override val heading: Angle get() = Angle(robot.radarHeadingRadians)

        override fun setTurn(angle: Angle) {
            robot.setTurnRadarRightRadians(angle.radians)
        }
    }
}

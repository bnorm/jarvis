package bnorm.parts.radar

import bnorm.parts.RobotPart
import robocode.AdvancedRobot
import kotlin.math.PI

const val RADAR_MAX_TURN = PI / 4 // robocode.Rules.RADAR_TURN_RATE_RADIANS

interface Radar : RobotPart {
    val heading: Double

    fun setTurn(radians: Double)
}

fun Radar(robot: AdvancedRobot): Radar {
    return object : Radar, RobotPart by RobotPart(robot) {
        override val heading: Double get() = robot.radarHeadingRadians

        override fun setTurn(radians: Double) {
            robot.setTurnRadarRightRadians(radians)
        }
    }
}

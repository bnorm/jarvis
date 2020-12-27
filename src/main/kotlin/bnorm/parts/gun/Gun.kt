package bnorm.parts.gun

import bnorm.parts.RobotPart
import robocode.AdvancedRobot
import kotlin.math.PI

const val GUN_MAX_TURN = PI / 9 // robocode.Rules.GUN_TURN_RATE_RADIANS

interface Gun : RobotPart {
    val heading: Double
    val energy: Double
    val heat: Double

    fun setTurn(radians: Double)
    fun setFire(power: Double)
}

fun Gun(robot: AdvancedRobot): Gun {
    return object : Gun, RobotPart by RobotPart(robot) {
        override val heading: Double get() = robot.gunHeadingRadians
        override val energy: Double get() = robot.energy
        override val heat: Double get() = robot.gunHeat

        override fun setTurn(radians: Double) {
            robot.setTurnRadarRightRadians(radians)
        }

        override fun setFire(power: Double) {
            val bullet = robot.setFireBullet(power)
        }
    }
}

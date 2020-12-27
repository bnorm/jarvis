package bnorm.parts

import robocode.AdvancedRobot

interface RobotPart {
    val x: Double
    val y: Double
    val time: Long

    val battleField: BattleField
}

fun RobotPart(robot: AdvancedRobot): RobotPart {
    return object : RobotPart {
        override val x: Double get() = robot.x
        override val y: Double get() = robot.y
        override val time: Long get() = robot.time

        override val battleField: BattleField by lazy {
            BattleField(robot.battleFieldWidth, robot.battleFieldHeight)
        }
    }
}

package bnorm.robot

import bnorm.Vector

data class RobotScan(
    val location: Vector,
    val velocity: Vector,
    val energy: Double,
    val time: Long,
    val interpolated: Boolean
)

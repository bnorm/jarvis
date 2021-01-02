package bnorm.robot

import bnorm.Vector

data class RobotScan(
    val location: Vector.Cartesian,
    val velocity: Vector.Polar,
    val energy: Double,
    val damage: Double,
    val time: Long,
    val interpolated: Boolean
) {
    var prev: RobotScan? = null
}

fun RobotScan.history(contiguous: Boolean = true): Sequence<RobotScan> {
    return generateSequence(this) { curr ->
        curr.prev?.takeIf { !contiguous || curr.time - it.time == 1L }
    }
}

package bnorm.robot

import bnorm.Vector
import bnorm.geo.Rectangle
import bnorm.parts.tank.TANK_SIZE
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class RobotScan(
    val location: Vector.Cartesian,
    val velocity: Vector.Polar,
    val energy: Double,
    val damage: Double,
    val time: Long,
    val interpolated: Boolean
) {
    @Transient
    var prev: RobotScan? = null

    @Transient
    val tank = Rectangle(location, TANK_SIZE, TANK_SIZE)
}

fun RobotScan.history(contiguous: Boolean = true): Sequence<RobotScan> {
    return generateSequence(this) { curr ->
        curr.prev?.takeIf { !contiguous || curr.time - it.time == 1L }
    }
}

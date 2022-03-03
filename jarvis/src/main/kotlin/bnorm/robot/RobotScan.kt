package bnorm.robot

import bnorm.Vector
import bnorm.geo.Rectangle
import bnorm.parts.tank.TANK_SIZE
import bnorm.robot.snapshot.BulletSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class RobotScan(
    val location: Vector.Cartesian,
    val velocity: Vector.Polar,
    val energy: Double,
    val time: Long,
    val interpolated: Boolean,
    val bulletHit: BulletSnapshot? = null,
    val hitByBullet: BulletSnapshot? = null,
) {
    @Transient
    var prev: RobotScan? = null

    @Transient
    val tank = Rectangle(location, TANK_SIZE, TANK_SIZE)
}

val RobotScan.damage: Double get() = hitByBullet?.damage ?: 0.0

fun RobotScan.history(contiguous: Boolean = true): Sequence<RobotScan> {
    return generateSequence(this) { curr ->
        curr.prev?.takeIf { !contiguous || curr.time - it.time == 1L }
    }
}

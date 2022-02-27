package bnorm.robot.snapshot

import bnorm.Vector
import bnorm.geo.Angle
import kotlinx.serialization.Serializable
import robocode.Bullet
import robocode.Rules

@Serializable
data class BulletSnapshot(
    val location: Vector.Cartesian,
    val velocity: Vector.Polar,
    val owner: String,
    val victim: String?,
) {
    // speed = 20 - 3 * power
    // power = (speed - 20) / 3
    val power: Double get() =
        (velocity.r - 20.0) / 3.0

    val damage: Double get() =
        Rules.getBulletDamage(power)
}

fun Bullet.toSnapshot() = BulletSnapshot(
    location = Vector.Cartesian(x, y),
    velocity = Vector.Polar(Angle(headingRadians), velocity),
    owner = name,
    victim = victim,
)

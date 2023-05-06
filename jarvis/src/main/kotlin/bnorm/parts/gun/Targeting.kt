package bnorm.parts.gun

import bnorm.Vector
import robocode.AdvancedRobot
import robocode.Bullet

interface Targeting {
    operator fun invoke(location: Vector.Cartesian): Vector.Polar

    fun onFire(bullet: Bullet?)
}

fun AdvancedRobot.target(
    targeting: Targeting,
    location: Vector.Cartesian
) {
    val target = targeting.invoke(location)

    // Only fire if there is no remaining movement from the previous turn
    val bullet = if (gunTurnRemainingRadians == 0.0) setFireBullet(target.r) else null
    targeting.onFire(bullet)

    setTurnGunRight(target.theta.radians - gunHeadingRadians)
}

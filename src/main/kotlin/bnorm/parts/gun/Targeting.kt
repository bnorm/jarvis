package bnorm.parts.gun

import bnorm.Vector
import robocode.AdvancedRobot
import robocode.Bullet
import kotlin.math.PI
import kotlin.math.abs

interface Targeting {
    suspend operator fun invoke(location: Vector.Cartesian): Vector.Polar

    suspend fun onFire(bullet: Bullet?)
}

suspend fun AdvancedRobot.target(
    targeting: Targeting,
    location: Vector.Cartesian
) {
    val target = targeting.invoke(location)
    setTurnGunRight(target.theta - gunHeadingRadians)

    // Only fire if the newly predicted angle is close to the old predicted angle
    val bullet = if (abs(gunTurnRemainingRadians) <= (PI / 360)) setFireBullet(target.r) else null
    targeting.onFire(bullet)
}

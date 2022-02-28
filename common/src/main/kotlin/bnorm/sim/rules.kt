package bnorm.sim

import bnorm.geo.Angle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

val ANGLE_UP = Angle(0 * PI / 2)
val ANGLE_RIGHT = Angle(1 * PI / 2)
val ANGLE_DOWN = Angle(2 * PI / 2)
val ANGLE_LEFT = Angle(3 * PI / 2)

const val TANK_SIZE = 36.0

val TANK_MAX_TURN = Angle(0.17453292519943295)
const val TANK_MAX_SPEED = 8.0
const val TANK_ACCELERATION = 1.0
const val TANK_DECELERATION = 2.0

val GUN_MAX_TURN = Angle(0.3490658503988659)
const val GUN_MIN_BULLET_POWER = 0.1
const val GUN_MAX_BULLET_POWER = 3.0

val RADAR_MAX_TURN = Angle(0.7853981633974483)
const val RADAR_SCAN_RADIUS = 1200.0

//const val ROBOT_HIT_DAMAGE = 0.6
//const val ROBOT_HIT_BONUS = 1.2

fun getTankTurnRate(velocity: Double): Angle {
    return TANK_MAX_TURN - Angle(0.013089969389957472 * abs(velocity))
}

fun getWallHitDamage(velocity: Double): Double {
    return max(abs(velocity) / 2.0 - 1.0, 0.0)
}

fun getBulletDamage(bulletPower: Double): Double {
    var damage = 4.0 * bulletPower
    if (bulletPower > 1.0) {
        damage += 2.0 * (bulletPower - 1.0)
    }
    return damage
}

fun getBulletHitBonus(bulletPower: Double): Double {
    return 3.0 * bulletPower
}

fun getBulletSpeed(bulletPower: Double): Double {
    var bulletPower = bulletPower
    bulletPower = min(max(bulletPower, 0.1), 3.0)
    return 20.0 - 3.0 * bulletPower
}

fun getGunHeat(bulletPower: Double): Double {
    return 1.0 + bulletPower / 5.0
}

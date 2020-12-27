package bnorm.parts.gun

import bnorm.Vector
import bnorm.robot.Robot

class DirectTargetingStrategy(
    private val gun: Gun
) : TargetingStrategy {
    override fun predict(robot: Robot, bulletPower: Double): Vector {
        return robot.history.latest.location.minus(gun.x, gun.y)
    }
}

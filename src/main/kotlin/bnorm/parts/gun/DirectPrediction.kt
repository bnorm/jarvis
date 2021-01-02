package bnorm.parts.gun

import bnorm.Vector
import bnorm.robot.Robot

class DirectPrediction(
    private val gun: Gun
) : Prediction {
    override fun predict(robot: Robot, bulletPower: Double): Vector {
        return robot.latest.location.minus(gun.x, gun.y)
    }
}

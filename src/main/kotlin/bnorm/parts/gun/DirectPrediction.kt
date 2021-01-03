package bnorm.parts.gun

import bnorm.Vector
import bnorm.robot.Robot

class DirectPrediction(
    private val self: Robot
) : Prediction {
    override fun predict(robot: Robot, bulletPower: Double): Vector {
        return robot.latest.location - self.latest.location
    }
}

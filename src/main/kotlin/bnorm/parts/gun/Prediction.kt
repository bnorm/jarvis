package bnorm.parts.gun

import bnorm.Vector
import bnorm.robot.Robot

interface Prediction {
    fun predict(robot: Robot, bulletPower: Double): Vector
}

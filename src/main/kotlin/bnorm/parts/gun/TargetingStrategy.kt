package bnorm.parts.gun

import bnorm.Vector
import bnorm.robot.Robot

interface TargetingStrategy {
    fun predict(robot: Robot, bulletPower: Double): Vector
}

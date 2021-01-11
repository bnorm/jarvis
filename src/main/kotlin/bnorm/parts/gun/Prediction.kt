package bnorm.parts.gun

import bnorm.Polar
import bnorm.Vector
import bnorm.robot.Robot
import bnorm.theta
import robocode.Bullet

interface Prediction {
    suspend fun predict(robot: Robot, bulletPower: Double): Vector
}

fun Prediction.toTargeting(robot: Robot, powerFunction: () -> Double): Targeting {
    return object : Targeting {
        override suspend fun invoke(location: Vector.Cartesian): Vector.Polar {
            val power = powerFunction()
            val prediction = predict(robot, power)
            return Polar(location.theta(prediction), power)
        }

        override suspend fun onFire(bullet: Bullet?) = Unit
    }
}

fun Prediction.toTargeting(robot: Robot, power: Double): Targeting {
    return object : Targeting {
        override suspend fun invoke(location: Vector.Cartesian): Vector.Polar {
            val prediction = predict(robot, power)
            return Polar(location.theta(prediction), power)
        }

        override suspend fun onFire(bullet: Bullet?) = Unit
    }
}

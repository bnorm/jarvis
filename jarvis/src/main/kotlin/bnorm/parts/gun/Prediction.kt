package bnorm.parts.gun

import bnorm.Polar
import bnorm.Vector
import bnorm.theta
import robocode.Bullet

interface Prediction {
    fun predict(bulletPower: Double): Vector
}

fun Prediction.toTargeting(powerFunction: () -> Double): Targeting {
    return object : Targeting {
        override fun invoke(location: Vector.Cartesian): Vector.Polar {
            val power = powerFunction()
            val prediction = predict(power)
            return Polar(location.theta(prediction), power)
        }

        override fun onFire(bullet: Bullet?) = Unit
    }
}

fun Prediction.toTargeting(power: Double): Targeting {
    return object : Targeting {
        override fun invoke(location: Vector.Cartesian): Vector.Polar {
            val prediction = predict(power)
            return Polar(location.theta(prediction), power)
        }

        override fun onFire(bullet: Bullet?) = Unit
    }
}

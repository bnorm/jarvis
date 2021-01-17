package bnorm.parts.gun

import bnorm.Vector
import bnorm.robot.Robot
import kotlin.random.Random

class DirectPrediction(
    private val self: Robot,
    private val robot: Robot,
) : Prediction {
    override fun predict(bulletPower: Double): Vector {
        // Some bots (*cough* DrussGT *cough*) like to bullet shield by causing bullet collision
        // Add a bit of fuzz to head on targeting so bullets do not collide
        val fuzz = Vector.Cartesian(Random.nextDouble(-1.0, 1.0), Random.nextDouble(-1.0, 1.0))
        return robot.latest.location - self.latest.location + fuzz
    }
}

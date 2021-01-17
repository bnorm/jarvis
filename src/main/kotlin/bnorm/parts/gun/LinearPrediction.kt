package bnorm.parts.gun

import bnorm.Vector
import bnorm.r2
import bnorm.robot.Robot
import bnorm.sqr
import robocode.Rules

class LinearPrediction(
    private val self: Robot,
    private val robot: Robot,
) : Prediction {
    override fun predict(bulletPower: Double): Vector {
        val enemyLocations = self.generateSequence(robot) { _, curr ->
            curr.copy(
                location = curr.location + curr.velocity,
                time = curr.time + 1,
            )
        }

        val source = self.latest.location
        val timeOffset = robot.latest.time - self.latest.time
        val bulletVelocity = Rules.getBulletSpeed(bulletPower)

        val predictedScan = enemyLocations.filterIndexed { index, predicted ->
            val bulletDistance = sqr((index + timeOffset) * bulletVelocity)
            val enemyDistance = source.r2(predicted.location)
            bulletDistance > enemyDistance
        }.first()

        return predictedScan.location - source
    }
}

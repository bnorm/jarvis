package bnorm.parts.gun

import bnorm.Vector
import bnorm.r2
import bnorm.robot.Robot
import bnorm.sqr
import robocode.Rules

class LinearPrediction(
    private val gun: Gun
) : Prediction {
    override fun predict(robot: Robot, bulletPower: Double): Vector {
        val enemyLocations = gun.generateSequence(robot) { _, curr ->
            curr.copy(
                location = curr.location + curr.velocity,
                time = curr.time + 1,
            )
        }

        val x = gun.x
        val y = gun.y
        val timeOffset = robot.latest.time - gun.time
        val bulletVelocity = Rules.getBulletSpeed(bulletPower)

        val predictedScan = enemyLocations.filterIndexed { index, predicted ->
            val bulletDistance = sqr((index + timeOffset) * bulletVelocity)
            val enemyDistance = r2(x, y, predicted.location.x, predicted.location.y)
            bulletDistance > enemyDistance
        }.first()

        return predictedScan.location.minus(x, y)
    }
}

package bnorm.parts.gun

import bnorm.Polar
import bnorm.Vector
import bnorm.parts.tank.TANK_MAX_SPEED
import bnorm.r2
import bnorm.robot.Robot
import bnorm.sqr
import robocode.Rules

class CircularPrediction(
    private val self: Robot
) : Prediction {
    override suspend fun predict(robot: Robot, bulletPower: Double): Vector {
        val enemyLocations = self.generateSequence(robot) { prev, curr ->
            curr.copy(
                location = curr.location + curr.velocity,
                velocity = Polar(
                    theta = 2 * curr.velocity.theta - prev.velocity.theta,
                    r = (2 * curr.velocity.r - prev.velocity.r).coerceIn(-TANK_MAX_SPEED, TANK_MAX_SPEED)
                ),
                time = curr.time + 1,
            )
        }

        val source = self.latest.location
        val bulletVelocity = Rules.getBulletSpeed(bulletPower)

        val timeOffset = robot.latest.time - self.latest.time
        val predictedScan = enemyLocations.filterIndexed { index, predicted ->
            val bulletDistance = sqr((index + timeOffset) * bulletVelocity)
            val enemyDistance = source.r2(predicted.location)
            bulletDistance > enemyDistance
        }.first()

        return predictedScan.location - source
    }
}

package bnorm.parts.gun

import bnorm.Polar
import bnorm.Vector
import bnorm.parts.tank.TANK_MAX_SPEED
import bnorm.r2
import bnorm.robot.Robot
import bnorm.sqr
import robocode.Rules

class CircularPrediction(
    private val gun: Gun
) : Prediction {
    override fun predict(robot: Robot, bulletPower: Double): Vector {
        val enemyLocations = gun.generateSequence(robot) { prev, curr ->
            curr.copy(
                location = curr.location + curr.velocity,
                velocity = Polar(
                    theta = 2 * curr.velocity.theta - prev.velocity.theta,
                    r = (2 * curr.velocity.r - prev.velocity.r).coerceIn(-TANK_MAX_SPEED, TANK_MAX_SPEED)
                ),
                time = curr.time + 1,
            )
        }

        val currTime = gun.time
        val x = gun.x
        val y = gun.y
        val bulletVelocity = Rules.getBulletSpeed(bulletPower)

        val timeOffset = robot.latest.time - currTime
        val predictedScan = enemyLocations.filterIndexed { index, predicted ->
            val bulletDistance = sqr((index + timeOffset) * bulletVelocity)
            val enemyDistance = r2(x, y, predicted.location.x, predicted.location.y)
            bulletDistance > enemyDistance
        }.first()

        return predictedScan.location.minus(x, y)
    }
}

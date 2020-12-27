package bnorm.parts.gun

import bnorm.Polar
import bnorm.Vector
import bnorm.parts.tank.TANK_MAX_VELOCITY
import bnorm.r2
import bnorm.robot.Robot
import bnorm.sqr
import bnorm.truncate
import robocode.Rules

class LinearTargetingStrategy(
    private val gun: Gun
) : TargetingStrategy {
    override fun predict(robot: Robot, bulletPower: Double): Vector {
        val enemyLocations = gun.generateSequence(robot) { prev, curr ->
            curr.copy(
                location = curr.location + curr.velocity,
                velocity = Polar(
                    theta = curr.velocity.theta,
                    r = truncate(-TANK_MAX_VELOCITY, 2 * curr.velocity.r - prev.velocity.r, TANK_MAX_VELOCITY)
                ),
                time = curr.time + 1,
            )
        }

        val currTime = gun.time
        val x = gun.x
        val y = gun.y
        val bulletVelocity = Rules.getBulletSpeed(bulletPower)

        val timeOffset = robot.history.latest.time - currTime
        val predictedScan = enemyLocations.filterIndexed { index, predicted ->
            val bulletDistance = sqr((index + timeOffset) * bulletVelocity)
            val enemyDistance = r2(x, y, predicted.location.x, predicted.location.y)
            bulletDistance > enemyDistance
        }.first()

        return predictedScan.location.minus(x, y)
    }
}

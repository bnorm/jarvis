package bnorm.parts.gun

import bnorm.Polar
import bnorm.Vector
import bnorm.kdtree.KdTree
import bnorm.parts.gun.virtual.escapeAngle
import bnorm.robot.Robot
import bnorm.robot.RobotSnapshots
import bnorm.sqr
import bnorm.theta
import robocode.Rules

class AntiGuessFactorPrediction<T : GuessFactorSnapshot>(
    private val self: Robot,
    private val positiveFunction: (Robot) -> Collection<KdTree.Neighbor<T>>,
    private val negativeFunction: (Robot) -> Collection<KdTree.Neighbor<T>>,
) : Prediction {
    override suspend fun predict(robot: Robot, bulletPower: Double): Vector {
        val escapeAngle = escapeAngle(self, robot, Rules.getBulletSpeed(bulletPower))
//        val distance = r(gun.x, gun.y, robot.latest.location)
//        val robotAngle = robotAngle(distance)

        val heading = theta(self.latest.location, robot.latest.location)
        val rotationDirection = robot.context[RobotSnapshots].latest.rotateDirection // TODO
        val buckets = buckets(positiveFunction(robot), negativeFunction(robot), 61)

        // TODO instead of buckets, use robotAngle to find the angle which the most number of angles match

        var max = 0.0
        var index = (buckets.size - 1) / 2
        for (i in buckets.indices) {
            val value = buckets[i]
            if (value > max) {
                max = value
                index = i
            }
        }

        val gf = index.toGuessFactor(buckets.size)
        val bearing = rotationDirection * gf * if (gf < 0) escapeAngle.reverse else escapeAngle.forward
        return Polar(heading + bearing, 1.0)
    }
}

fun buckets(
    positive: Iterable<KdTree.Neighbor<GuessFactorSnapshot>>,
    negative: Iterable<KdTree.Neighbor<GuessFactorSnapshot>>,
    bucketCount: Int,
    width: Int = 3
): DoubleArray {
    val sum = DoubleArray(bucketCount)

    positive.forEach {
        val bucket = it.value.guessFactor.toBucket(bucketCount)
        sum[bucket] += sqr(width + 1.0) / (1 + it.dist)

        for (i in 1..width) {
            if (bucket + i < bucketCount) {
                sum[bucket + i] += sqr(width + 1.0 - i) / (1 + it.dist)
            }
            if (bucket - i >= 0) {
                sum[bucket - i] += sqr(width + 1.0 - i) / (1 + it.dist)
            }
        }
    }

    negative.forEach {
        val bucket = it.value.guessFactor.toBucket(bucketCount)
        sum[bucket] -= 2 * sqr(width + 1.0) / (1 + it.dist)

        for (i in 1..width) {
            if (bucket + i < bucketCount) {
                sum[bucket + i] -= 2 * sqr(width + 1.0 - i) / (1 + it.dist)
            }
            if (bucket - i >= 0) {
                sum[bucket - i] -= 2 * sqr(width + 1.0 - i) / (1 + it.dist)
            }
        }
    }

    return sum
}

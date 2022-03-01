package bnorm.parts.gun

import bnorm.Polar
import bnorm.Vector
import bnorm.geo.times
import bnorm.kdtree.KdTree
import bnorm.parts.tank.escape.escape
import bnorm.robot.Robot
import bnorm.robot.snapshot
import bnorm.theta
import robocode.Rules

class AntiGuessFactorPrediction<T : GuessFactorSnapshot>(
    private val self: Robot,
    private val robot: Robot,
    private val positiveFunction: (Robot) -> Collection<KdTree.Neighbor<T>>,
    private val negativeFunction: (Robot) -> Collection<KdTree.Neighbor<T>>,
) : Prediction {
    override fun predict(bulletPower: Double): Vector {
        val source = self.latest.location
        val target = robot.latest.location
        val escapeAngle = self.battleField.escape(source, target, Rules.getBulletSpeed(bulletPower))
//        val distance = r(gun.x, gun.y, robot.latest.location)
//        val robotAngle = robotAngle(distance)

        val heading = theta(source, target)
        val direction = robot.snapshot.gfDirection
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

        val gf = direction * index.toGuessFactor(buckets.size)
        val bearing = gf * if (gf < 0) escapeAngle.leftAngle else escapeAngle.rightAngle
        return Polar(heading + bearing, 1.0)
    }
}

fun buckets(
    positive: Iterable<KdTree.Neighbor<GuessFactorSnapshot>>,
    negative: Iterable<KdTree.Neighbor<GuessFactorSnapshot>>,
    bucketCount: Int
): DoubleArray {
    val buckets = DoubleArray(bucketCount)

    for (b in buckets.indices) {
        val gf = b.toGuessFactor(bucketCount)
        for (point in positive) {
            buckets[b] += gauss(1.0 / point.dist, point.value.guessFactor, 0.1, gf)
        }
        for (point in negative) {
            buckets[b] += gauss(10.0 / point.dist, point.value.guessFactor, 0.3, gf)
            buckets[b] -= gauss(10.0 / point.dist, point.value.guessFactor, 0.1, gf)
        }
    }
    return buckets
}

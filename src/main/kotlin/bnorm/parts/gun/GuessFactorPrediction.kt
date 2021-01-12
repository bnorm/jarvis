package bnorm.parts.gun

import bnorm.Polar
import bnorm.Vector
import bnorm.kdtree.KdTree
import bnorm.parts.gun.virtual.escapeAngle
import bnorm.parts.tank.TANK_SIZE
import bnorm.robot.Robot
import bnorm.robot.snapshot
import bnorm.sqr
import bnorm.theta
import robocode.Rules
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.roundToInt

interface GuessFactorSnapshot {
    val guessFactor: Double
}

class GuessFactorPrediction<T : GuessFactorSnapshot>(
    private val self: Robot,
    private val clusterFunction: (Robot) -> Collection<KdTree.Neighbor<T>>,
) : Prediction {
    override suspend fun predict(robot: Robot, bulletPower: Double): Vector {
        val escapeAngle = escapeAngle(self, robot, Rules.getBulletSpeed(bulletPower))
//        val distance = r(gun.x, gun.y, robot.latest.location)
//        val robotAngle = robotAngle(distance)

        val heading = theta(self.latest.location, robot.latest.location)
        val rotationDirection = robot.snapshot.rotateDirection // TODO
        val cluster = clusterFunction(robot)
        val buckets = cluster.buckets(61)

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

fun Int.toGuessFactor(bucketCount: Int): Double {
    val halfBucketCount = (bucketCount - 1) / 2
    return (this - halfBucketCount).toDouble() / halfBucketCount
}

fun Double.toBucket(bucketCount: Int): Int =
    ((bucketCount - 1.0) / 2.0 * (this + 1)).roundToInt()

fun robotAngle(distance: Double): Double {
    return asin(TANK_SIZE / distance)
}

fun Iterable<KdTree.Neighbor<GuessFactorSnapshot>>.buckets(bucketCount: Int): DoubleArray {
    val buckets = DoubleArray(bucketCount)

    for (b in buckets.indices) {
        val gf = b.toGuessFactor(bucketCount)
        for (point in this) {
            buckets[b] += gauss(1.0 / point.dist, point.value.guessFactor, 0.1, gf)
        }
    }

    return buckets
}

fun gauss(
    a: Double, // Height
    b: Double, // Center
    c: Double, // Standard Deviation
    x: Double
) = a * exp(-sqr(b - x) / (2.0 * sqr(c)))

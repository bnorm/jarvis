package bnorm.parts.gun

import bnorm.Polar
import bnorm.Vector
import bnorm.geo.times
import bnorm.kdtree.KdTree
import bnorm.parts.tank.TANK_SIZE
import bnorm.parts.tank.escape.escape
import bnorm.robot.Robot
import bnorm.robot.snapshot
import bnorm.sqr
import bnorm.theta
import bnorm.trace
import robocode.Rules
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.roundToInt

interface GuessFactorSnapshot {
    val guessFactor: Double
}

class GuessFactorPrediction<T : GuessFactorSnapshot>(
    private val self: Robot,
    private val robot: Robot,
    private val clusterFunction: (Robot) -> Collection<KdTree.Neighbor<T>>,
) : Prediction {
    override fun predict(bulletPower: Double): Vector {
        trace("gf") {
            val source = self.latest.location
            val target = robot.latest.location
            val escapeAngle = self.battleField.escape(source, target, Rules.getBulletSpeed(bulletPower))

//        val robotAngle = robotAngle(r(gun.x, gun.y, robot.latest.location))

            val heading = theta(source, target)
            val direction = robot.snapshot.gfDirection
            val cluster = clusterFunction(robot)
            val buckets = cluster.buckets(31)

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

    trace("buckets") {
        for (b in buckets.indices) {
            val gf = b.toGuessFactor(bucketCount)
            for (point in this) {
                buckets[b] += gauss(1.0 / point.dist, point.value.guessFactor, 0.1, gf)
            }
        }
    }

    return buckets
}

inline fun gauss(
    a: Double, // Height
    b: Double, // Center
    c: Double, // Standard Deviation
    x: Double
) = a / exp(sqr(b - x) / (2.0 * c * c))

inline fun gauss(
    a: Double, // Height
    c: Double, // Standard Deviation
    x: Double
) = a / exp(sqr(x) / (2.0 * c * c))

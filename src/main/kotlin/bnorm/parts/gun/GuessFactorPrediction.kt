package bnorm.parts.gun

import bnorm.Polar
import bnorm.Vector
import bnorm.kdtree.KdTree
import bnorm.parts.tank.TANK_MAX_SPEED
import bnorm.parts.tank.TANK_SIZE
import bnorm.robot.Robot
import bnorm.robot.RobotScan
import bnorm.signMul
import bnorm.sqr
import bnorm.theta
import robocode.Rules
import robocode.util.Utils
import kotlin.math.asin
import kotlin.math.roundToInt

interface GuessFactorSnapshot {
    val guessFactor: Double
}

class GuessFactorPrediction<T : GuessFactorSnapshot>(
    private val gun: Gun,
    private val clustering: KdTree<T>,
    private val snapshotFunction: (Robot) -> T,
) : Prediction {
    override fun predict(robot: Robot, bulletPower: Double): Vector {
        val escapeAngle = escapeAngle(Rules.getBulletSpeed(bulletPower))
//        val distance = r(gun.x, gun.y, robot.latest.location)
//        val robotAngle = robotAngle(distance)

        val heading = theta(gun.x, gun.y, robot.latest.location)
        val rotationDirection = rotationDirection(heading, robot.latest)

        val buckets = clustering.neighbors(snapshotFunction(robot))
            .take(100)
            .buckets(61)

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
        return Polar(heading + rotationDirection * gf * escapeAngle, 1.0)
    }
}

fun Int.toGuessFactor(bucketCount: Int): Double {
    val halfBucketCount = (bucketCount - 1) / 2
    return (this - halfBucketCount).toDouble() / halfBucketCount
}

fun Double.toBucket(bucketCount: Int): Int =
    ((bucketCount - 1.0) / 2.0 * (this + 1)).roundToInt()

fun escapeAngle(speed: Double): Double {
    return asin(TANK_MAX_SPEED / speed)
}

fun robotAngle(distance: Double): Double {
    return asin(TANK_SIZE / distance)
}

fun rotationDirection(heading: Double, scan: RobotScan): Double {
    val robotBearing = Utils.normalRelativeAngle(scan.velocity.theta - heading)
    val robotSpeed = generateSequence(scan) { it.prev }
        .map { it.velocity.r }.firstOrNull { it != 0.0 } ?: 0.0
    return signMul(robotBearing) * signMul(robotSpeed)
}

fun Sequence<KdTree.Neighbor<GuessFactorSnapshot>>.buckets(bucketCount: Int, width: Int = 1): DoubleArray {
    val sum = DoubleArray(bucketCount)
//    val weightSum = DoubleArray(bucketCount)

    forEach {
        val bucket = it.value.guessFactor.toBucket(bucketCount)
        sum[bucket] += sqr(width + 1.0) / (1 + it.dist)
//        weightSum[bucket] += it.dist

        for (i in 1..width) {
            if (bucket + i < bucketCount) {
                sum[bucket + i] += sqr(width + 1.0 - i) / (1 + it.dist)
//                weightSum[bucket] += it.dist
            }
            if (bucket - i >= 0) {
                sum[bucket - i] += sqr(width + 1.0 - i) / (1 + it.dist)
//                weightSum[bucket] += it.dist
            }
        }
    }

//    for (i in sum.indices) {
//        if (weightSum[i] != 0.0) {
//            sum[i] /= weightSum[i]
//        }
//    }

    return sum
}

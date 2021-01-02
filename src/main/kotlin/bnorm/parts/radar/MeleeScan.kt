package bnorm.parts.radar

import bnorm.robot.Robot
import bnorm.theta
import robocode.util.Utils
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign

class MeleeScan(
    private val radar: Radar,
    private val aliveRobots: Collection<Robot>,
) : Scan {
    private var turningRight = true

    override fun setMove() {
        if (aliveRobots.isEmpty()) {
            // If there are no known alive robots, spin infinitely
            radar.setTurn(Double.POSITIVE_INFINITY)
            return
        }

        val currTime = radar.time
        val robotAngles = sortedSetOf<Double>()
        for (robot in aliveRobots) {
            val scan = robot.latest
            val angle = theta(radar.x, radar.y, scan.location)
            val escapeAngle = radar.escapeAngle(currTime + 2, scan)

            robotAngles.add(Utils.normalAbsoluteAngle(angle - escapeAngle))
            robotAngles.add(Utils.normalAbsoluteAngle(angle + escapeAngle))
        }

        //             x
        // next -> x   |   x <- prev
        //          \  |  /
        //           \ | /
        //             R
        //         <- gap ->
        //
        // Need to turn *right* to the *min*
        // Need to turn *left* to the *max*

        var prev = robotAngles.last()
        var maxGap = 0.0
        var rightRobotAngle = 0.0
        var leftRobotAngle = 0.0
        for (next in robotAngles) {
            val gap = Utils.normalAbsoluteAngle(next - prev)
            if (gap > maxGap) {
                maxGap = gap
                rightRobotAngle = prev
                leftRobotAngle = next
            }

            // If the gap is bigger than *half* a circle, there cannot be a bigger gap
            if (maxGap > PI) break
            prev = next
        }

        if (maxGap > PI) {
            val rightBearing = Utils.normalRelativeAngle(rightRobotAngle - radar.heading)
            val leftBearing = Utils.normalRelativeAngle(leftRobotAngle - radar.heading)

            val turnAngle = when {
                rightBearing == 0.0 -> leftBearing
                leftBearing == 0.0 -> rightBearing
                sign(rightBearing) == sign(leftBearing) -> {
                    // Both bearings are in the same direction, turn towards the farthest
                    turningRight = rightBearing > 0
                    sign(rightBearing) * maxOf(abs(rightBearing), abs(leftBearing))
                }
                turningRight -> rightBearing
                else -> leftBearing
            }

            radar.setTurn(turnAngle)

            if (abs(turnAngle) < RADAR_MAX_TURN) {
                // We can complete the required turn this tick so turn the other direction next tick
                turningRight = !turningRight
            }
        } else {
            radar.setTurn(if (turningRight) Double.POSITIVE_INFINITY else Double.NEGATIVE_INFINITY)
        }
    }
}

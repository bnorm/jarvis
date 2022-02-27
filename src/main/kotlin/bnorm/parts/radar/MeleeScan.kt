package bnorm.parts.radar

import bnorm.geo.Angle
import bnorm.geo.abs
import bnorm.geo.normalizeAbsolute
import bnorm.geo.normalizeRelative
import bnorm.geo.sign
import bnorm.geo.times
import bnorm.robot.Robot
import bnorm.sim.RADAR_MAX_TURN
import bnorm.theta

class MeleeScan(
    private val radar: Radar,
    private val aliveRobots: Collection<Robot>,
) : Scan {
    private var turningRight = true

    override fun setMove() {
        if (aliveRobots.isEmpty()) {
            // If there are no known alive robots, spin infinitely
            radar.setTurn(Angle.POSITIVE_INFINITY)
            return
        }

        val currTime = radar.time
        val robotAngles = sortedSetOf<Angle>()
        for (robot in aliveRobots) {
            val scan = robot.latest
            val angle = theta(radar.x, radar.y, scan.location)
            val escapeAngle = radar.escapeAngle(currTime + 2, scan)

            robotAngles.add((angle - escapeAngle).normalizeAbsolute())
            robotAngles.add((angle + escapeAngle).normalizeAbsolute())
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
        var maxGap = Angle.ZERO
        var rightRobotAngle = Angle.ZERO
        var leftRobotAngle = Angle.ZERO
        for (next in robotAngles) {
            val gap = (next - prev).normalizeAbsolute()
            if (gap > maxGap) {
                maxGap = gap
                rightRobotAngle = prev
                leftRobotAngle = next
            }

            // If the gap is bigger than *half* a circle, there cannot be a bigger gap
            if (maxGap > Angle.HALF_CIRCLE) break
            prev = next
        }

        if (maxGap > Angle.HALF_CIRCLE) {
            val rightBearing = (rightRobotAngle - radar.heading).normalizeRelative()
            val leftBearing = (leftRobotAngle - radar.heading).normalizeRelative()

            val turnAngle = when {
                rightBearing == Angle.ZERO -> leftBearing
                leftBearing == Angle.ZERO -> rightBearing
                sign(rightBearing) == sign(leftBearing) -> {
                    // Both bearings are in the same direction, turn towards the farthest
                    turningRight = rightBearing > Angle.ZERO
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
            radar.setTurn(if (turningRight) Angle.POSITIVE_INFINITY else Angle.NEGATIVE_INFINITY)
        }
    }
}

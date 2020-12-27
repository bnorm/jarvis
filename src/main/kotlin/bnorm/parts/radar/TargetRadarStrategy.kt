package bnorm.parts.radar

import bnorm.parts.RobotPart
import bnorm.parts.tank.TANK_MAX_VELOCITY
import bnorm.r
import bnorm.robot.Robot
import bnorm.robot.RobotScan
import bnorm.theta
import robocode.util.Utils
import kotlin.math.sign

class TargetRadarStrategy(
    private val radar: Radar,
    private val robot: Robot,
) : RadarStrategy {
    override fun setMove() {
        val scan = robot.history.latest
        val escapeAngle = radar.escapeAngle(radar.time - scan.time + 2, scan)

        if (escapeAngle > RADAR_MAX_TURN / 2) {
            // Been too long since observed, spin infinitely to find
            radar.setTurn(Double.POSITIVE_INFINITY)
        } else {
            val robotBearing = Utils.normalRelativeAngle(theta(radar.x, radar.y, scan.location) - radar.heading)

            // TODO if abs(robotBearing) < escapeAngle -> robot could be on either side of the radar

            val radarTurn = robotBearing + sign(robotBearing) * escapeAngle
            radar.setTurn(radarTurn)
        }
    }
}

fun RobotPart.escapeAngle(timeDiff: Long, scan: RobotScan): Double {
    val escapeDistance = (timeDiff + 1) * TANK_MAX_VELOCITY
    val robotDistance = r(x, y, scan.location)
    return theta(escapeDistance, robotDistance)
}

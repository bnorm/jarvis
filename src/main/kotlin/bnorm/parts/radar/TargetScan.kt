package bnorm.parts.radar

import bnorm.parts.RobotPart
import bnorm.parts.tank.TANK_MAX_SPEED
import bnorm.r
import bnorm.robot.Robot
import bnorm.robot.RobotScan
import bnorm.signMul
import bnorm.theta
import robocode.util.Utils

class TargetScan(
    private val radar: Radar,
    private val robot: Robot,
) : Scan {
    override fun setMove() {
        val scan = robot.latest
        val escapeAngle = radar.escapeAngle(radar.time + 2, scan)

        if (escapeAngle > RADAR_MAX_TURN / 2) {
            // Been too long since observed, spin infinitely to find
            radar.setTurn(Double.POSITIVE_INFINITY)
        } else {
            val robotBearing = Utils.normalRelativeAngle(theta(radar.x, radar.y, scan.location) - radar.heading)

            // TODO if abs(robotBearing) < escapeAngle -> robot could be on either side of the radar

            val radarTurn = robotBearing + signMul(robotBearing) * escapeAngle
            radar.setTurn(radarTurn)
        }
    }
}

fun RobotPart.escapeAngle(time: Long, scan: RobotScan): Double {
    // Add 2 to the time diff for when moving parallel at max speed in opposite directions
    val escapeDistance = (time - scan.time + 2) * TANK_MAX_SPEED
    val robotDistance = r(x, y, scan.location)
    return theta(escapeDistance, robotDistance)
}

package bnorm.parts.radar

import bnorm.geo.Angle
import bnorm.geo.normalizeRelative
import bnorm.geo.times
import bnorm.parts.RobotPart
import bnorm.parts.tank.TANK_MAX_SPEED
import bnorm.r
import bnorm.robot.Robot
import bnorm.robot.RobotScan
import bnorm.signMul
import bnorm.sim.RADAR_MAX_TURN
import bnorm.theta

class TargetScan(
    private val radar: Radar,
    private val robot: Robot,
) : Scan {
    override fun setMove() {
        val scan = robot.latest
        val escapeAngle = radar.escapeAngle(radar.time + 2, scan)

        if (escapeAngle > RADAR_MAX_TURN / 2.0) {
            // Been too long since observed, spin infinitely to find
            radar.setTurn(Angle.POSITIVE_INFINITY)
        } else {
            val robotBearing = (theta(radar.x, radar.y, scan.location) - radar.heading).normalizeRelative()

            // TODO if abs(robotBearing) < escapeAngle -> robot could be on either side of the radar

            val radarTurn = robotBearing + signMul(robotBearing.radians) * escapeAngle
            radar.setTurn(radarTurn)
        }
    }
}

fun RobotPart.escapeAngle(time: Long, scan: RobotScan): Angle {
    // Add 2 to the time diff for when moving parallel at max speed in opposite directions
    val escapeDistance = (time - scan.time + 2) * TANK_MAX_SPEED
    val robotDistance = r(x, y, scan.location)
    return theta(escapeDistance, robotDistance)
}

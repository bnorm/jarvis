package bnorm

import bnorm.kdtree.KdTree
import bnorm.parts.gun.buckets
import bnorm.parts.gun.rotationDirection
import bnorm.parts.gun.toGuessFactor
import bnorm.parts.gun.virtual.Wave
import bnorm.parts.gun.virtual.escapeAngle
import bnorm.parts.gun.virtual.radius
import bnorm.parts.tank.TANK_MAX_SPEED
import bnorm.parts.tank.TANK_SIZE
import bnorm.robot.RobotScan
import bnorm.robot.RobotSnapshot
import robocode.Rules
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.asin

fun Graphics2D.drawLine(start: Vector, end: Vector) {
    drawLine(start.x.toInt(), start.y.toInt(), end.x.toInt(), end.y.toInt())
}

fun Graphics2D.fillCircle(it: Vector, diameter: Int) {
    fillOval(it.x.toInt(), it.y.toInt(), diameter, diameter)
}

fun Graphics2D.drawWave(self: RobotScan, wave: Wave<WaveData>, time: Long) {
//    color = Color.blue
    val radius = wave.radius(time)
//    drawCircle(wave.origin, radius)
//
//    color = Color.yellow
    val heading = theta(wave.origin, wave.value.scan.location)
//    val escapeAngle = wave.escapeAngle(TANK_MAX_SPEED)
//    drawLine(wave.origin, wave.origin + Polar(heading - escapeAngle, radius))
//    drawLine(wave.origin, wave.origin + Polar(heading + escapeAngle, radius))
//
    color = Color.red
    val location = wave.origin + Polar(heading, radius)
    drawCircle(location, TANK_SIZE / 2)
    drawLine(location, location + wave.value.scan.velocity)

    drawCluster(self, wave.value.scan, radius, wave.value.cluster)
}

fun Graphics2D.drawCircle(center: Vector, radius: Double) {
    drawOval((center.x - radius).toInt(), (center.y - radius).toInt(), (2 * radius).toInt(), (2 * radius).toInt())
}

fun Graphics2D.drawBox(center: Vector, side: Double) {
    val halfSide = side / 2
    drawRect((center.x - halfSide).toInt(), (center.y - halfSide).toInt(), side.toInt(), side.toInt())
}

fun Graphics2D.drawCluster(
    self: RobotScan,
    scan: RobotScan,
    radius: Double,
    neighbors: List<KdTree.Neighbor<RobotSnapshot>>,
) {
    val buckets = neighbors.asSequence().buckets(31)

    val max = buckets.maxOrNull()!!
    val min = buckets.minOrNull()!!

    //            println("buckets=${buckets.map { (99 * (it - min) / (max - min)).toInt() }}")

    val theta = theta(self.location, scan.location)
    val escapeAngle = asin(TANK_MAX_SPEED / Rules.getBulletSpeed(3.0))
    val rotationDirection = rotationDirection(theta, scan)

    for (i in buckets.indices) {
        val green = (255 * (buckets[i] - min) / (max - min)).toInt()
        color = Color(255 - green, green, 0)
        val gf = i.toGuessFactor(buckets.size)
        //                if (buckets[i] == max) {
        //                    println("gf = $gf -> ${buckets.map { it }}")
        //                    println(neighbors.map { it.value.guessFactor })
        //                }
        fillCircle(self.location + Polar(theta + rotationDirection * gf * escapeAngle, radius), 4)
    }
}

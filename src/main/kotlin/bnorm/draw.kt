package bnorm

import bnorm.parts.gun.CircularPrediction
import bnorm.parts.gun.DirectPrediction
import bnorm.parts.gun.GuessFactorPrediction
import bnorm.parts.gun.LinearPrediction
import bnorm.parts.gun.virtual.VirtualGun
import bnorm.parts.gun.buckets
import bnorm.parts.gun.toGuessFactor
import bnorm.parts.gun.virtual.Wave
import bnorm.parts.gun.virtual.radius
import bnorm.parts.tank.TANK_MAX_SPEED
import bnorm.robot.RobotScan
import bnorm.robot.RobotSnapshot
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.asin

fun Graphics2D.drawLine(start: Vector, end: Vector) {
    drawLine(start.x.toInt(), start.y.toInt(), end.x.toInt(), end.y.toInt())
}

fun Graphics2D.fillCircle(it: Vector, diameter: Int) {
    fillOval(it.x.toInt(), it.y.toInt(), diameter, diameter)
}

fun Graphics2D.drawWave(self: RobotScan, wave: Wave<WaveData<RobotSnapshot>>, time: Long) {
//    color = Color.blue
//    val radius = wave.radius(time)
//    drawCircle(wave.origin, radius)
//
//    color = Color.yellow
//    val heading = theta(wave.origin, wave.value.scan.location)
//    val escapeAngle = wave.escapeAngle(TANK_MAX_SPEED)
//    drawLine(wave.origin, wave.origin + Polar(heading - escapeAngle, radius))
//    drawLine(wave.origin, wave.origin + Polar(heading + escapeAngle, radius))
//
//    color = Color.red
//    val location = wave.origin + Polar(heading, radius)
//    drawCircle(location, TANK_SIZE / 2)
//    drawLine(location, location + wave.value.scan.velocity)

    drawCluster(wave, time)
}

fun Graphics2D.drawBullets(gun: VirtualGun, time: Long) {
    color = when (gun.prediction) {
        is DirectPrediction -> Color.red
        is LinearPrediction -> Color.orange
        is CircularPrediction -> Color.yellow
        is GuessFactorPrediction<*> -> Color.green
        else -> Color.white
    }

    for (bullet in gun.bullets) {
        drawLine(bullet.location(time - 1), bullet.location(time))
    }
}

fun Graphics2D.drawSuccess(index: Int, gun: VirtualGun) {
    color = when (gun.prediction) {
        is DirectPrediction -> Color.red
        is LinearPrediction -> Color.orange
        is CircularPrediction -> Color.yellow
        is GuessFactorPrediction<*> -> Color.green
        else -> Color.white
    }

    val success = (100 * gun.success).toInt()
    fillRect(0, 10 * index, success, 10)
    drawRect(success, 10 * index, 100 - success, 10)

    color = Color.white
    drawString("${gun.success.roundDecimals(3)}%", 110, 10 * index)
}

fun Graphics2D.drawCircle(center: Vector, radius: Double) {
    drawOval((center.x - radius).toInt(), (center.y - radius).toInt(), (2 * radius).toInt(), (2 * radius).toInt())
}

fun Graphics2D.drawBox(center: Vector, side: Double) {
    val halfSide = side / 2
    drawRect((center.x - halfSide).toInt(), (center.y - halfSide).toInt(), side.toInt(), side.toInt())
}

fun Graphics2D.drawCluster(wave: Wave<WaveData<RobotSnapshot>>, time: Long) {
    val radius = wave.radius(time)
    val buckets = wave.value.cluster.buckets(31)

    val max = buckets.maxOrNull()!!
    val min = buckets.minOrNull()!!

    //            println("buckets=${buckets.map { (99 * (it - min) / (max - min)).toInt() }}")

    val heading = theta(wave.origin, wave.value.scan.location)
    val escapeAngle = asin(TANK_MAX_SPEED / wave.speed)
    val rotationDirection = wave.value.snapshot.rotateDirection

    for (i in buckets.indices) {
        val green = (255 * (buckets[i] - min) / (max - min)).toInt()
        color = Color(0, green, 255 - green)
        val gf = i.toGuessFactor(buckets.size)
        fillCircle(wave.origin + Polar(heading + rotationDirection * gf * escapeAngle, radius), if (wave.value.real) 8 else 3)
    }
}

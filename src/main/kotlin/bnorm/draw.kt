package bnorm

import bnorm.geo.Circle
import bnorm.geo.Line
import bnorm.geo.Rectangle
import bnorm.geo.intersect
import bnorm.geo.times
import bnorm.parts.gun.CircularPrediction
import bnorm.parts.gun.DirectPrediction
import bnorm.parts.gun.GuessFactorPrediction
import bnorm.parts.gun.LinearPrediction
import bnorm.parts.gun.buckets
import bnorm.parts.gun.toGuessFactor
import bnorm.parts.gun.virtual.VirtualGun
import bnorm.parts.gun.virtual.Wave
import bnorm.parts.gun.virtual.radius
import bnorm.parts.tank.Movement
import bnorm.parts.tank.OrbitMovement
import bnorm.parts.tank.WallSmoothMovement
import bnorm.parts.tank.escape.EscapeEnvelope
import bnorm.parts.tank.escape.escapeAngle
import bnorm.parts.tank.simulate
import bnorm.plugin.get
import bnorm.robot.Robot
import bnorm.robot.RobotScan
import bnorm.robot.snapshot.WallProbe
import bnorm.sim.ANGLE_DOWN
import bnorm.sim.ANGLE_LEFT
import bnorm.sim.ANGLE_RIGHT
import bnorm.sim.ANGLE_UP
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import robocode.Rules
import java.awt.Color
import java.awt.Graphics2D

fun Graphics2D.draw(circle: Circle) {
    drawCircle(circle.center, circle.radius)
}

fun Graphics2D.draw(rectangle: Rectangle) {
    drawRect(
        (rectangle.center.x - rectangle.width / 2).toInt(),
        (rectangle.center.y - rectangle.height / 2).toInt(),
        rectangle.width.toInt(),
        rectangle.height.toInt()
    )
}

fun Graphics2D.drawLine(start: Vector.Cartesian, end: Vector.Cartesian) {
    drawLine(start.x.toInt(), start.y.toInt(), end.x.toInt(), end.y.toInt())
}

fun Graphics2D.drawLine(start: Vector.Cartesian, direction: Vector.Polar) {
    drawLine(start, start + direction)
}

fun Graphics2D.fillCircle(location: Vector, diameter: Int) {
    fillOval((location.x - diameter / 2.0).toInt(), (location.y - diameter / 2.0).toInt(), diameter, diameter)
}

fun Graphics2D.drawProbe(location: Vector.Cartesian, direction: Vector.Polar, diameter: Int = 4) {
    drawLine(location, direction)
    fillCircle(location + direction, diameter)
}

fun Graphics2D.drawWave(self: RobotScan, wave: Wave, time: Long) {
    color = Color.blue
    val radius = wave.radius(time)
    drawCircle(wave.origin, radius)

    color = Color.yellow
    val heading = wave.context[WaveHeading]
    val escapeEnvelope = wave.context[EscapeEnvelope.key]
    drawLine(wave.origin, wave.origin + Polar(heading - escapeEnvelope.leftAngle, radius))
    drawLine(wave.origin, wave.origin + Polar(heading + escapeEnvelope.rightAngle, radius))

//    color = Color.red
//    val location = wave.origin + Polar(heading, radius)
//    drawCircle(location, TANK_SIZE / 2)
//    drawLine(location, location + wave.value.scan.velocity)

//    drawCluster(wave, time)
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
    drawString("${gun.success.roundDecimals(3)}% ${gun.name}", 110, 10 * index)
}

fun Graphics2D.drawCircle(center: Vector, radius: Double) {
    drawOval((center.x - radius).toInt(), (center.y - radius).toInt(), (2 * radius).toInt(), (2 * radius).toInt())
}

fun Graphics2D.drawBox(center: Vector, side: Double) {
    val halfSide = side / 2
    drawRect((center.x - halfSide).toInt(), (center.y - halfSide).toInt(), side.toInt(), side.toInt())
}

fun Graphics2D.draw(
    envelope: EscapeEnvelope,
    source: Vector.Cartesian = envelope.source,
    target: Vector.Cartesian = envelope.target,
    buckets: DoubleArray? = null,
    direction: Int = 1,
) {
    color = Color.blue
    draw(envelope)

    if (buckets == null) {
        color = Color.red
        for (p in envelope) {
            drawLine(envelope.target, p)
        }
    } else {
        val max = buckets.maxOrNull()!!
        val min = buckets.minOrNull()!!

        val heading = theta(source, target)
        val circle = envelope.circle

        for (i in buckets.indices) {
            color = when (i) {
                0 -> Color.red
                buckets.lastIndex -> Color.green
                else -> {
                    val green = (255 * (buckets[i] - min) / (max - min)).toInt()
                    Color(0, green, 255 - green)
                }
            }

            val gf = direction * i.toGuessFactor(buckets.size)
            val bearing = gf * if (gf < 0) envelope.leftAngle else envelope.rightAngle

            val trajectory = Line(source, heading + bearing)
            val intersection = (circle intersect trajectory).toList()
            if (intersection.size == 2) {
                drawLine(intersection[0], intersection[1])
            }
        }
    }
}

fun Graphics2D.draw(envelope: EscapeEnvelope) {
    draw(envelope.circle)
    for (p in envelope) {
        drawLine(envelope.source, p)
    }
}

fun Graphics2D.drawCluster(wave: Wave, time: Long) {
    val radius = wave.radius(time)
    val buckets = if (wave.context[RealBullet]) {
        (wave.context.find(BulletCluster) ?: wave.context.find(RealCluster))?.buckets(31)
    } else {
        wave.context.find(VirtualCluster)?.buckets(31)
    }

    if (buckets == null) {
        color = Color.blue
        drawCircle(wave.origin, wave.radius(time))
        return
    }

    val max = buckets.maxOrNull()!!
    val min = buckets.minOrNull()!!

    //            println("buckets=${buckets.map { (99 * (it - min) / (max - min)).toInt() }}")

    val snapshot = wave[WaveSnapshot]
    val heading = theta(wave.origin, snapshot.scan.location)
    val escapeAngle = wave.escapeAngle
    val direction = snapshot.gfDirection

    val middle = (buckets.size - 1) / 2
    for (i in buckets.indices) {
        val threat: Double = (buckets[i] - min) / (max - min)
        color = when {
            i < middle -> Color.red
            i > middle -> Color.green
            else -> Color.yellow
        }

        val gf = direction * i.toGuessFactor(buckets.size)
        val bearing = gf * if (gf < 0) escapeAngle.leftAngle else escapeAngle.rightAngle
        fillCircle(
            wave.origin.project(heading + bearing, radius),
            ((if (wave.context[RealBullet]) 4 else 2) * threat).toInt()
        )
    }
}

fun Graphics2D.draw(
    wallProbe: WallProbe
) {
    val (north, east, south, west) = wallProbe.position
    val location = Cartesian(west, south)

    color = Color.white
    drawProbe(location, Polar(ANGLE_UP, north), 8)
    drawProbe(location, Polar(ANGLE_RIGHT, east), 8)
    drawProbe(location, Polar(ANGLE_DOWN, south), 8)
    drawProbe(location, Polar(ANGLE_LEFT, west), 8)

    color = Color.green
    drawProbe(location, wallProbe.heading.forward(), 8)
    drawProbe(location, wallProbe.perpendicular.forward(), 8)

    color = Color.red
    drawProbe(location, wallProbe.heading.backward(), 8)
    drawProbe(location, wallProbe.perpendicular.backward(), 8)
}

fun Graphics2D.draw(robot: Robot, movement: Movement) = runBlocking {
    robot.simulate(movement)
        .take(25)
        .collect { fillCircle(it, 4) }
}

fun Graphics2D.drawPath(source: Robot, target: Robot, moveDirection: Int) = runBlocking {
    val sourceLocation = source.latest.location
    val speed = Rules.getBulletSpeed(3.0)

//    run {
//        color = Color.green
//        val movement = WallSmoothMovement(
//            target.battleField,
//            OrbitMovement(source, 500.0, 1.0 * moveDirection)
//        )
//        var time = 0
//        target.simulate(movement)
//            .takeWhile { sqr(time++ * speed) <= sourceLocation.r2(it) }
//            .collect { drawCircle(it, 2.0) }
//    }

    run {
        color = Color.red
        val movement = WallSmoothMovement(
            target.battleField,
            OrbitMovement(source, 500.0, -1.0 * moveDirection)
        )
        var time = 0
        target.simulate(movement)
            .takeWhile { sqr(time++ * speed) <= sourceLocation.r2(it) }
            .collect { drawCircle(it, 2.0) }
    }
}

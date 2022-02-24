package bnorm.parts.tank.escape

import bnorm.Vector
import bnorm.geo.Circle
import bnorm.geo.contains
import bnorm.geo.intersect
import bnorm.geo.tangentPoints
import bnorm.parts.BattleField
import bnorm.parts.gun.virtual.Wave
import bnorm.parts.gun.virtual.WaveContext
import bnorm.parts.tank.TANK_MAX_SPEED
import bnorm.r
import bnorm.sqr
import bnorm.theta
import robocode.util.Utils
import kotlin.math.sqrt

data class EscapeEnvelope(
    val leftPoint: Vector.Cartesian,
    val leftAngle: Double,
    val rightPoint: Vector.Cartesian,
    val rightAngle: Double,
    val source: Vector.Cartesian,
    val target: Vector.Cartesian,
    val speed: Double,
    val circle: Circle,
) {
    companion object : WaveContext.Key<EscapeEnvelope>

    operator fun iterator(): Iterator<Vector.Cartesian> = iterator {
        yield(leftPoint)
        yield(rightPoint)
    }
}

val Wave.escapeAngle: EscapeEnvelope get() = context[EscapeEnvelope]

fun escapeCircle(
    source: Vector.Cartesian,
    target: Vector.Cartesian,
    speed: Double,
): Circle {
    // https://robowiki.net/wiki/Escape_Circle
    val k = 1.0 / (sqr(speed / TANK_MAX_SPEED) - 1.0)
    return Circle(
        center = Vector.Cartesian(
            source.x + (1 + k) * (target.x - source.x),
            source.y + (1 + k) * (target.y - source.y)
        ),
        radius = source.r(target) * sqrt(k * k + k)
    )
}

fun BattleField.escape(source: Vector.Cartesian, target: Vector.Cartesian, speed: Double): EscapeEnvelope {
    var leftPoint = target
    var leftAngle = 0.0
    var rightPoint = target
    var rightAngle = 0.0

    val angle = source.theta(target)
    fun assign(t: Vector.Cartesian) {
        val tAngle = Utils.normalRelativeAngle(source.theta(t) - angle)
        if (tAngle < 0.0) {
            if (-tAngle > leftAngle) {
                leftPoint = t
                leftAngle = -tAngle
            }
        } else {
            if (tAngle > rightAngle) {
                rightPoint = t
                rightAngle = tAngle
            }
        }
    }

    val circle = escapeCircle(source, target, speed)
    for (t in (movable intersect circle)) {
        assign(t)
    }

    for (t in (source tangentPoints circle)) {
        if (t in movable) {
            assign(t)
        }
    }

    return EscapeEnvelope(leftPoint, leftAngle, rightPoint, rightAngle, source, target, speed, circle)
}

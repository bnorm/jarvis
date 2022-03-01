package bnorm.parts.tank.escape

import bnorm.Vector
import bnorm.geo.Angle
import bnorm.geo.Circle
import bnorm.geo.contains
import bnorm.geo.intersect
import bnorm.geo.normalizeRelative
import bnorm.geo.tangentPoints
import bnorm.parts.BattleField
import bnorm.parts.gun.virtual.Wave
import bnorm.parts.tank.TANK_MAX_SPEED
import bnorm.plugin.Context
import bnorm.r
import bnorm.sqr
import bnorm.theta
import kotlin.math.sqrt

data class EscapeEnvelope(
    val leftPoint: Vector.Cartesian,
    val leftAngle: Angle,
    val rightPoint: Vector.Cartesian,
    val rightAngle: Angle,
    val source: Vector.Cartesian,
    val target: Vector.Cartesian,
    val speed: Double,
    val circle: Circle,
) {
    companion object {
        val key = Context.Key<EscapeEnvelope>("EscapeEnvelope")
    }

    operator fun iterator(): Iterator<Vector.Cartesian> = iterator {
        yield(leftPoint)
        yield(rightPoint)
    }
}


val Wave.escapeAngle: EscapeEnvelope get() = context[EscapeEnvelope.key]

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
    var leftAngle = Angle.ZERO
    var rightPoint = target
    var rightAngle = Angle.ZERO

    val angle = source.theta(target)
    fun assign(t: Vector.Cartesian) {
        val tAngle = (source.theta(t) - angle).normalizeRelative()
        if (tAngle < Angle.ZERO) {
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

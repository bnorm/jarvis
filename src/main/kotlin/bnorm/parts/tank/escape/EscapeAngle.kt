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
import kotlin.math.abs
import kotlin.math.sqrt

data class EscapeAngle(
    val leftPoint: Vector.Cartesian,
    val leftAngle: Double,
    val rightPoint: Vector.Cartesian,
    val rightAngle: Double,
) {
    companion object : WaveContext.Feature<EscapeAngle>

    operator fun iterator(): Iterator<Vector.Cartesian> = iterator {
        yield(leftPoint)
        yield(rightPoint)
    }
}

val Wave.escapeAngle: EscapeAngle get() = context[EscapeAngle]

fun escapeCircle(
    source: Vector.Cartesian,
    target: Vector.Cartesian,
    speed: Double,
): Circle {
    // https://robowiki.net/wiki/Escape_Circle
    val k = 1.0 / (sqr(speed / TANK_MAX_SPEED) - 1.0)
    val circle = Circle(
        center = Vector.Cartesian(
            source.x + (1 + k) * (target.x - source.x),
            source.y + (1 + k) * (target.y - source.y)
        ),
        radius = source.r(target) * sqrt(k * k + k)
    )
    return circle
}

//  Iterative : 57.6us
//  This      :  2.82us
fun BattleField.escape(source: Vector.Cartesian, target: Vector.Cartesian, speed: Double): EscapeAngle {
    var leftPoint = target
    var leftAngle = 0.0
    var rightPoint = target
    var rightAngle = 0.0

    val angle = source.theta(target)
    fun assign(t: Vector.Cartesian) {
        val tAngle = Utils.normalRelativeAngle(source.theta(t) - angle)
        if (tAngle < 0) {
            leftPoint = t
            leftAngle = abs(tAngle)
        } else {
            rightPoint = t
            rightAngle = tAngle
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

    return EscapeAngle(leftPoint, leftAngle, rightPoint, rightAngle)
}

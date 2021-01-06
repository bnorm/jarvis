package bnorm.parts.tank.escape

import bnorm.Vector
import bnorm.parts.tank.TANK_MAX_SPEED
import bnorm.r
import bnorm.sqr
import kotlin.math.sqrt

// https://robowiki.net/wiki/Escape_Circle
data class EscapeCircle(
    val center: Vector.Cartesian,
    val radius: Double,
) {
    companion object {
        fun from(source: Vector.Cartesian, target: Vector.Cartesian, speed: Double): EscapeCircle {
            val k = 1.0 / (sqr(speed / TANK_MAX_SPEED) - 1.0)
            return EscapeCircle(
                center = Vector.Cartesian(
                    source.x + (1 + k) * (target.x - source.x),
                    source.y + (1 + k) * (target.y - source.y)
                ),
                radius = source.r(target) * sqrt(k * k + k)
            )
        }
    }
}

package bnorm.geo

import bnorm.Vector
import kotlin.math.asin

data class Line(
    val p1: Vector.Cartesian,
    val p2: Vector.Cartesian,
) {
    init {
        require(p1 != p2) { "p1=$p1 p2=$p2" }
    }
}

fun Line(p: Vector.Cartesian, angle: Double) =
    Line(p, Vector.Cartesian(p.x + 1.0, p.y + asin(angle)))


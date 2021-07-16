package bnorm.geo

import bnorm.Vector
import bnorm.normalAbsoluteAngle
import kotlin.math.PI
import kotlin.math.tan

data class Line(
    val m: Double,
    val b: Double,
) {
    // For a vertical line, formula is x = b
    val vertical: Boolean get() = m.isNaN()
}

fun Line(p1: Vector.Cartesian, p2: Vector.Cartesian): Line {
    // Vertical line
    if (p1.x == p2.x) return Line(Double.NaN, p1.x)

    // y = m * x + b -> b = y - m * x
    // y1 - m * x1 = y2 - m * x2
    // y1 - y2 = m * (x1 - x2)
    // m = (y1 - y2) / (x1 - x2)
    val m = (p1.y - p2.y) / (p1.x - p2.x)

    // y = m * x + b -> m = (y - b) / x
    // y1 = ((y2 - b) / x2) * x1 + b
    // y1 = (y2 * x1 - b * x1) / x2 + b
    // y1 - (y2 * x1) / x2 = b - (b * x1) / x2
    // (y1 * x2) - (y2 * x1) = (b * x2) - (b * x1)
    // b * (x2 - x1) = (y1 * x2) - (y2 * x1)
    // b = ((y1 * x2) - (y2 * x1)) / (x2 - x1)
    val b = ((p1.y * p2.x) - (p2.y * p1.x)) / (p2.x - p1.x)

    return Line(m, b)
}

fun Line(p: Vector.Cartesian, angle: Double): Line {
    // Vertical line
    val absolute = normalAbsoluteAngle(angle)
    if (absolute % PI == 0.0) return Line(Double.NaN, p.x)

    // y = m * x + b
    val m = tan(PI / 2 - absolute) // Robocode angle -> Polar angle
    val b = p.y - (m * p.x)
    return Line(m, b)
}

fun Line.f(x: Double) = Vector.Cartesian(x, m * x + b)

operator fun Line.contains(p: Vector.Cartesian): Boolean =
   if (vertical) p.x == b else m * p.x + b == p.y

package bnorm.geo

import bnorm.Vector

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
    // b = y1 - m * x1
    val b = p1.y - m * p1.x

    return Line(m, b)
}

fun Line(p: Vector.Cartesian, angle: Angle): Line {
    // Vertical line
    val absolute = angle.normalizeAbsolute()
    if (absolute % Angle.HALF_CIRCLE == Angle.ZERO) return Line(Double.NaN, p.x)

    // y = m * x + b
    val m = tan(Angle.QUARTER_CIRCLE - absolute) // Robocode angle -> Polar angle
    val b = p.y - (m * p.x)
    return Line(m, b)
}

fun Line.f(x: Double) = Vector.Cartesian(x, m * x + b)

operator fun Line.contains(p: Vector.Cartesian): Boolean =
    if (vertical) p.x == b else m * p.x + b == p.y

infix fun Line.intersect(line: Line): Vector.Cartesian? {
    if (this == line) return null // the same line
    if (this.vertical && line.vertical) return null // both vertical and not equal

    // If one line is vertical, intersection is easily calculated
    if (this.vertical) return line.f(this.b)
    if (line.vertical) return this.f(line.b)

    // y = m1 * x + b1
    // y = m2 * x + b2
    // m1 * x + b1 = m2 * x + b2
    // x = (b2 - b1) / (m1 - m2)

    return this.f((line.b - b) / (m - line.m))
}

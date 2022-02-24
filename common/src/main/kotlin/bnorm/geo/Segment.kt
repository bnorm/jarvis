package bnorm.geo

import bnorm.Vector

data class Segment(
    val p1: Vector.Cartesian,
    val p2: Vector.Cartesian,
)

operator fun Segment.contains(p: Vector.Cartesian): Boolean {
    return contains(p.x, p.y)
}

fun Segment.contains(x: Double, y: Double): Boolean {
    // Vertical segment
    if (p1.x == p2.x) return x == p1.x
    if (x !in minOf(p1.x, p2.x)..maxOf(p1.x, p2.x) ||
        y !in minOf(p1.y, p2.y)..maxOf(p1.y, p2.y)
    ) return false

    // y = m * x + b -> b = y - m * x
    // y1 - m * x1 = y2 - m * x2
    // y1 - y2 = m * (x1 - x2)
    // m = (y1 - y2) / (x1 - x2)
    val m = (p1.y - p2.y) / (p1.x - p2.x)

    // y = m * x + b
    return y - m * x == p1.y - m * p1.x
}

infix fun Segment.intersect(segment: Segment): Vector.Cartesian? {
    // https://www.geeksforgeeks.org/program-for-point-of-intersection-of-two-lines/

    // a1 * x + b1 * y = c1
    // a2 * x + b2 * y = c2

    val a1 = p2.y - p1.y
    val b1 = p1.x - p2.x

    val a2 = segment.p2.y - segment.p1.y
    val b2 = segment.p1.x - segment.p2.x

    val determinant = a1 * b2 - a2 * b1

    return if (determinant == 0.0) null // parallel
    else {
        val c1 = a1 * p1.x + b1 * p1.y
        val c2 = a2 * segment.p1.x + b2 * segment.p1.y

        val x = (b2 * c1 - b1 * c2) / determinant
        val y = (a1 * c2 - a2 * c1) / determinant

        if (
            x in minOf(p1.x, p2.x)..maxOf(p1.x, p2.x) &&
            y in minOf(p1.y, p2.y)..maxOf(p1.y, p2.y) &&
            x in minOf(segment.p1.x, segment.p2.x)..maxOf(segment.p1.x, segment.p2.x) &&
            y in minOf(segment.p1.y, segment.p2.y)..maxOf(segment.p1.y, segment.p2.y)
        ) {
            Vector.Cartesian(x, y)
        } else {
            null
        }
    }
}

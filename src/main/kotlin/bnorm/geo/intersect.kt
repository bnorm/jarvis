@file:Suppress("NOTHING_TO_INLINE")

package bnorm.geo

import bnorm.Vector
import bnorm.project
import bnorm.r
import bnorm.r2
import bnorm.sqr
import bnorm.theta
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.sqrt

infix fun Rectangle.intersect(circle: Circle): Set<Vector.Cartesian> {
    val points = mutableSetOf<Vector.Cartesian>()

    // circle: (x - x0)^2 + (y - y0)^2 = r^2
    // x = x0 +- sqrt(r^2 - (y - y0)^2)
    // y = y0 +- sqrt(r^2 - (x - x0)^2)

    val x0 = circle.center.x
    val y0 = circle.center.y
    val r2 = sqr(circle.radius)

    fun solveY(x: Double) {
        val ySum2 = (r2 - sqr(x - x0))
        if (ySum2 == 0.0) {
            if (y0 in yRange) points.add(Vector.Cartesian(x, y0))
        } else if (ySum2 > 0.0) {
            val ySum = sqrt(ySum2)
            val yTop = y0 + ySum
            val yBottom = y0 - ySum
            if (yTop in yRange) points.add(Vector.Cartesian(x, yTop))
            if (yBottom in yRange) points.add(Vector.Cartesian(x, yBottom))
        }
    }

    fun solveX(y: Double) {
        val xSum2 = (r2 - sqr(y - y0))
        if (xSum2 == 0.0) {
            if (x0 in xRange) points.add(Vector.Cartesian(x0, y))
        } else if (xSum2 > 0.0) {
            val xSum = sqrt(xSum2)
            val xRight = x0 + xSum
            val xLeft = x0 - xSum
            if (xRight in xRange) points.add(Vector.Cartesian(xRight, y))
            if (xLeft in xRange) points.add(Vector.Cartesian(xLeft, y))
        }
    }

    solveY(xRange.start) // left
    solveY(xRange.endInclusive) // right
    solveX(yRange.start) // bottom
    solveX(yRange.endInclusive) // top

    return points
}

inline infix fun Circle.intersect(rectangle: Rectangle) = rectangle.intersect(this)

// Determine the tangent lines from a source point which touch a circle

infix fun Circle.tangents(p: Vector.Cartesian): Set<Line> {
    val r2 = center.r2(p)
    if (r2 < radius * radius) return emptySet()

    val angle = center.theta(p)
    if (r2 == radius * radius) return setOf(Line(p, angle + PI / 2))

    val delta = acos(radius / sqrt(r2))
    return setOf(
        Line(p, center.project(angle + delta, radius)),
        Line(p, center.project(angle - delta, radius)),
    )
}

inline infix fun Vector.Cartesian.tangents(circle: Circle) =
    circle.tangents(this)

infix fun Circle.tangentPoints(p: Vector.Cartesian): Set<Vector.Cartesian> {
    val r2 = center.r2(p)
    if (r2 < radius * radius) return emptySet()
    if (r2 == radius * radius) return setOf(p)

    val angle = center.theta(p)
    val delta = acos(radius / sqrt(r2))
    return setOf(
        center.project(angle + delta, radius),
        center.project(angle - delta, radius),
    )
}

inline infix fun Vector.Cartesian.tangentPoints(circle: Circle) =
    circle.tangentPoints(this)

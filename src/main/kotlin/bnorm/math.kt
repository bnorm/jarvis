@file:Suppress("NOTHING_TO_INLINE")

package bnorm

import kotlin.math.*

fun normalize(min: Double, value: Double, max: Double): Double {
    return 2.0 * (value - min) / (max - min) - 1.0
}
fun Double.project(min: Double, max: Double): Double {
    return 2.0 * (this - min) / (max - min) - 1.0
}

fun signMul(n: Double): Double {
    return if (n < 0.0) -1.0 else 1.0
}

inline fun sqr(d: Double): Double {
    return d * d
}

inline fun theta(x: Double, y: Double): Double =
    atan2(x, y)

inline fun theta(x1: Double, y1: Double, x2: Double, y2: Double): Double =
    atan2(x2 - x1, y2 - y1)

inline fun theta(source: Vector, destination: Vector): Double =
    theta(source.x, source.y, destination.x, destination.y)

inline fun theta(x: Double, y: Double, destination: Vector): Double =
    theta(x, y, destination.x, destination.y)

inline fun r2(x1: Double, y1: Double, x2: Double, y2: Double): Double =
    sqr(x2 - x1) + sqr(y2 - y1)

inline fun r2(source: Vector, destination: Vector): Double =
    r2(source.x, source.y, destination.x, destination.y)

inline fun r2(x: Double, y: Double, destination: Vector): Double =
    r2(x, y, destination.x, destination.y)

inline fun r(x1: Double, y1: Double, x2: Double, y2: Double): Double =
    sqrt(r2(x1, y1, x2, y2))

inline fun r(source: Vector, destination: Vector): Double =
    r(source.x, source.y, destination.x, destination.y)

inline fun r(x: Double, y: Double, destination: Vector): Double =
    r(x, y, destination.x, destination.y)
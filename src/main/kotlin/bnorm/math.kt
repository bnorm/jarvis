@file:Suppress("NOTHING_TO_INLINE")

package bnorm

import kotlin.math.*

fun truncate(min: Double, value: Double, max: Double) = maxOf(minOf(value, max), min)

inline fun sqr(d: Double): Double {
    return d * d
}

inline fun theta(x1: Double, y1: Double, x2: Double, y2: Double): Double =
    atan2(x2 - x1, y2 - y1)

inline fun theta(source: Vector, destination: Vector): Double =
    theta(source.x, source.y, destination.x, destination.y)

inline fun r2(x1: Double, y1: Double, x2: Double, y2: Double): Double =
    sqr(x2 - x1) + sqr(y2 - y1)

inline fun r2(source: Vector, destination: Vector): Double =
    r2(source.x, source.y, destination.x, destination.y)

inline fun r(x1: Double, y1: Double, x2: Double, y2: Double): Double =
    sqrt(r2(x1, y1, x2, y2))

inline fun r(source: Vector, destination: Vector): Double =
    r(source.x, source.y, destination.x, destination.y)

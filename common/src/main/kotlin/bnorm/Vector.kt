package bnorm

import kotlinx.serialization.Serializable
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

sealed class Vector {
    abstract val x: Double
    abstract val y: Double

    abstract val theta: Double
    abstract val r: Double
    abstract val r2: Double

    operator fun plus(other: Vector) = Cartesian(this.x + other.x, this.y + other.y)
    fun plus(x: Double, y: Double) = Cartesian(this.x + x, this.y + y)

    operator fun minus(other: Vector) = Cartesian(this.x - other.x, this.y - other.y)
    fun minus(x: Double, y: Double) = Cartesian(this.x - x, this.y - y)

    operator fun times(scalar: Double) = Polar(theta, scalar * r)

    @Serializable
    data class Cartesian(override val x: Double, override val y: Double) : Vector() {
        override val theta: Double get() = atan2(x, y)
        override val r: Double get() = sqrt(x * x + y * y)
        override val r2: Double get() = x * x + y * y
    }

    @Serializable
    data class Polar(override val theta: Double, override val r: Double) : Vector() {
        override val x: Double get() = sin(theta) * r
        override val y: Double get() = cos(theta) * r
        override val r2: Double get() = r * r
    }
}

fun Vector.toCartesian() = this as? Vector.Cartesian ?: Vector.Cartesian(x, y)
fun Cartesian(x: Double, y: Double) = Vector.Cartesian(x, y)
fun Cartesian(x: Double, y: Double, theta: Double, r: Double) =
    Vector.Cartesian(x + sin(theta) * r, y + cos(theta) * r)

fun Vector.toPolar() = this as? Vector.Polar ?: Vector.Polar(theta, r)
fun Polar(theta: Double, r: Double) = Vector.Polar(theta, r)

fun Vector.project(theta: Double, r: Double) =
    Cartesian(x, y, theta, r)

fun Vector.theta(x: Double, y: Double): Double {
    return atan2(x - this.x, y - this.y)
}

fun Vector.theta(destination: Vector): Double {
    return atan2(destination.x - x, destination.y - y)
}

fun Vector.r2(x: Double, y: Double): Double {
    return sqr(x - this.x) + sqr(y - this.y)
}

fun Vector.r2(destination: Vector): Double {
    return sqr(destination.x - x) + sqr(destination.y - y)
}

fun Vector.r(x: Double, y: Double): Double {
    return sqrt(sqr(x - this.x) + sqr(y - this.y))
}

fun Vector.r(destination: Vector): Double {
    return sqrt(sqr(destination.x - x) + sqr(destination.y - y))
}

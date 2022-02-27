@file:Suppress("NOTHING_TO_INLINE")

package bnorm.geo

import bnorm.roundDecimals
import kotlinx.serialization.Serializable
import kotlin.math.PI

@Serializable
@JvmInline
value class Angle(
    val radians: Double
) : Comparable<Angle> {
    companion object {
        val ZERO = Angle(0.0)
        val QUARTER_CIRCLE = Angle(PI / 2)
        val HALF_CIRCLE = Angle(PI)
        val CIRCLE = Angle(2 * PI)
        val POSITIVE_INFINITY = Angle(Double.POSITIVE_INFINITY)
        val NEGATIVE_INFINITY = Angle(Double.NEGATIVE_INFINITY)
    }

    inline operator fun plus(angle: Angle) = Angle(radians + angle.radians)
    inline operator fun minus(angle: Angle) = Angle(radians - angle.radians)
    inline operator fun unaryMinus() = Angle(-radians)

    inline operator fun times(scalar: Double) = Angle(radians * scalar)

    inline operator fun div(scalar: Double) = Angle(radians / scalar)
    inline operator fun div(angle: Angle) = radians / angle.radians
    inline operator fun rem(angle: Angle) = Angle(radians % angle.radians)

    override fun compareTo(other: Angle) = radians.compareTo(other.radians)

    override fun toString(): String {
        return "${Math.toDegrees(radians).roundDecimals(1)}°"
    }

}

inline fun abs(angle: Angle) = Angle(kotlin.math.abs(angle.radians))
inline fun sign(angle: Angle) = kotlin.math.sign(angle.radians)

inline fun sin(angle: Angle) = kotlin.math.sin(angle.radians)
inline fun cos(angle: Angle) = kotlin.math.cos(angle.radians)
inline fun tan(angle: Angle) = kotlin.math.tan(angle.radians)

inline fun asin(x: Double): Angle = Angle(kotlin.math.asin(x))
inline fun acos(x: Double): Angle = Angle(kotlin.math.acos(x))
inline fun atan(x: Double): Angle = Angle(kotlin.math.atan(x))
inline fun atan2(y: Double, x: Double): Angle = Angle(kotlin.math.atan2(y, x))

operator fun Double.times(angle: Angle) = Angle(this * angle.radians)
// operator fun Double.div(angle: Angle) = Angle(this / radians)

fun Angle.normalizeAbsolute(): Angle {
    val normal = this.radians % (2 * PI)
    return Angle(if (normal >= 0.0) normal else normal + 2 * PI)
}

fun Angle.normalizeRelative(): Angle {
    val normal = this.radians % (2 * PI)
    return Angle(
        when {
            normal >= 0.0 -> when {
                normal < PI -> normal
                else -> normal - 2 * PI
            }
            else -> when {
                normal >= -PI -> normal
                else -> normal + 2 * PI
            }
        }
    )
}

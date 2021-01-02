package bnorm.parts.gun.virtual

import bnorm.Vector
import kotlin.math.asin

data class Wave<T>(
    val origin: Vector.Cartesian,
    val speed: Double,
    val time: Long,
    val value: T,
) {
    fun interface Listener<T> {
        fun onWave(wave: Wave<T>)
    }
}

fun Wave<*>.radius(time: Long): Double {
    return speed * (time - this.time)
}

fun Wave<*>.escapeAngle(speed: Double): Double {
    return asin(speed / this.speed)
}

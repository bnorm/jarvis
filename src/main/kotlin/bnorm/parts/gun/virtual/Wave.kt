package bnorm.parts.gun.virtual

import bnorm.Vector
import bnorm.geo.Circle
import bnorm.plugin.Context
import bnorm.plugin.ContextHolder

data class Wave(
    val origin: Vector.Cartesian,
    val speed: Double,
    val time: Long,
) : ContextHolder {
    override val context: Context = Context()
}

fun Wave.radius(time: Long): Double {
    return speed * (time - this.time)
}

fun Wave.toCircle(time: Long) =
    Circle(origin, radius(time))

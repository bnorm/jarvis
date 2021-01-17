package bnorm.parts.gun.virtual

import bnorm.Vector
import bnorm.geo.Circle
import bnorm.parts.tank.Movement
import bnorm.parts.tank.simulate
import bnorm.r2
import bnorm.robot.Robot
import bnorm.sqr
import bnorm.theta
import kotlinx.coroutines.flow.first
import kotlin.math.asin

data class Wave(
    val origin: Vector.Cartesian,
    val speed: Double,
    val time: Long,
    val context: WaveContext,
)

fun Wave.radius(time: Long): Double {
    return speed * (time - this.time)
}

fun Wave.toCircle(time: Long) =
    Circle(origin, radius(time))

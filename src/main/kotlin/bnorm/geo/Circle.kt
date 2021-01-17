package bnorm.geo

import bnorm.Vector
import bnorm.project
import bnorm.r
import bnorm.r2
import bnorm.theta
import kotlin.math.PI
import kotlin.math.acos

data class Circle(
    val center: Vector.Cartesian,
    val radius: Double,
)

operator fun Circle.contains(p: Vector.Cartesian) =
    center.r2(p) <= radius * radius

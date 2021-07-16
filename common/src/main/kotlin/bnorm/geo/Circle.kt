package bnorm.geo

import bnorm.Vector
import bnorm.r2

data class Circle(
    val center: Vector.Cartesian,
    val radius: Double,
)

operator fun Circle.contains(p: Vector.Cartesian) =
    center.r2(p) <= radius * radius

package bnorm.parts

import bnorm.Vector
import bnorm.r

data class BattleField(
    val width: Double,
    val height: Double,
) {
    val diagonal = r(0.0, 0.0, width, height)
}

fun BattleField.contains(x: Double, y: Double): Boolean =
    x in 0.0..width && y in 0.0..height

operator fun BattleField.contains(vector: Vector): Boolean =
    contains(vector.x, vector.y)

fun BattleField.contains(x: Double, y: Double, padding: Double): Boolean =
    x in padding..(width - padding) && y in padding..(height - padding)

fun BattleField.contains(vector: Vector, padding: Double): Boolean =
    contains(vector.x, vector.y, padding)
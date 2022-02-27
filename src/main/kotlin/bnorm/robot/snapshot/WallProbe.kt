package bnorm.robot.snapshot

import bnorm.Vector
import bnorm.geo.Angle
import bnorm.geo.cos
import bnorm.geo.normalizeAbsolute
import bnorm.parts.BattleField
import bnorm.sim.ANGLE_DOWN
import bnorm.sim.ANGLE_LEFT
import bnorm.sim.ANGLE_RIGHT
import bnorm.sqr
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.abs
import kotlin.math.sqrt

@Serializable
data class WallProbe(
    val position: Position,
    val heading: Movement,
    val perpendicular: Movement,
) {
    @Serializable
    data class Position(
        val north: Double,
        val east: Double,
        val south: Double,
        val west: Double,
    ) {
        @Transient
        val diagonal = sqrt(sqr(north + south) + sqr(east + west))
    }

    @Serializable
    data class Movement(
        val heading: Angle,
        val forward: Double,
        val backward: Double,
    ) {
        fun forward() = Vector.Polar(heading, forward)
        fun backward() = Vector.Polar(heading + Angle.HALF_CIRCLE, backward)
    }
}

fun WallProbe(
    battleField: BattleField,
    location: Vector.Cartesian,
    heading: Angle,
    perpendicular: Angle
): WallProbe {
    val position = position(battleField, location)
    return WallProbe(position, probe(position, heading), probe(position, perpendicular))
}

private fun position(
    battleField: BattleField,
    location: Vector.Cartesian
): WallProbe.Position {
    val west = location.x
    val south = location.y
    val east = battleField.width - west
    val north = battleField.height - south
    return WallProbe.Position(north, east, south, west)
}

private fun probe(
    position: WallProbe.Position,
    angle: Angle
): WallProbe.Movement {
    val heading = angle.normalizeAbsolute()
    val headingXWallDistance = (if (heading < ANGLE_DOWN) position.east else position.west) / abs(cos(ANGLE_RIGHT - heading))
    val reverseXWallDistance = (if (heading < ANGLE_DOWN) position.west else position.east) / abs(cos(ANGLE_RIGHT - heading))
    val headingYWallDistance = (if (heading in ANGLE_RIGHT..ANGLE_LEFT) position.south else position.north) / abs(cos(heading))
    val reverseYWallDistance = (if (heading in ANGLE_RIGHT..ANGLE_LEFT) position.north else position.south) / abs(cos(heading))
    return WallProbe.Movement(
        heading = heading,
        forward = minOf(headingXWallDistance, headingYWallDistance),
        backward = minOf(reverseXWallDistance, reverseYWallDistance)
    )
}

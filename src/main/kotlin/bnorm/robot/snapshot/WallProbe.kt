package bnorm.robot.snapshot

import bnorm.Vector
import bnorm.parts.BattleField
import bnorm.sqr
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import robocode.util.Utils
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
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
        val heading: Double,
        val forward: Double,
        val backward: Double,
    ) {
        fun forward() = Vector.Polar(heading, forward)
        fun backward() = Vector.Polar(heading + PI, backward)
    }
}

fun WallProbe(
    battleField: BattleField,
    location: Vector.Cartesian,
    heading: Double,
    perpendicular: Double
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
    angle: Double
): WallProbe.Movement {
    val heading = Utils.normalAbsoluteAngle(angle)
    val headingXWallDistance = (if (heading < PI) position.east else position.west) / abs(cos(PI / 2 - heading))
    val reverseXWallDistance = (if (heading < PI) position.west else position.east) / abs(cos(PI / 2 - heading))
    val headingYWallDistance = (if (heading in PI / 2..PI * 3 / 2) position.south else position.north) / abs(cos(heading))
    val reverseYWallDistance = (if (heading in PI / 2..PI * 3 / 2) position.north else position.south) / abs(cos(heading))
    return WallProbe.Movement(
        heading = heading,
        forward = minOf(headingXWallDistance, headingYWallDistance),
        backward = minOf(reverseXWallDistance, reverseYWallDistance)
    )
}

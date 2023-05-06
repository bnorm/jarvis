package bnorm.parts.tank

import bnorm.Polar
import bnorm.Vector
import bnorm.draw.Debug
import bnorm.draw.DebugKey
import bnorm.fillCircle
import bnorm.geo.Angle
import bnorm.geo.cos
import bnorm.parts.BattleField
import bnorm.parts.contains
import bnorm.r2
import bnorm.robot.Robot
import bnorm.robot.RobotScan
import bnorm.sqr
import bnorm.theta
import java.awt.Color
import kotlin.math.abs
import kotlin.math.sqrt

class MinimumRiskMovement(
    private val battleField: BattleField,
    private val aliveRobots: Collection<Robot>,
) : Movement {
    companion object {
        const val distanceValues = 3
        const val angleValues = 16
        val angleSection = Angle.CIRCLE / angleValues.toDouble()
    }

    private var destination: Vector.Cartesian? = null
    private var previous: Vector.Cartesian? = null

    override fun invoke(location: Vector.Cartesian, velocity: Vector.Polar): Vector.Polar {
        val scans = aliveRobots.map { it.latest }
        val closest = scans.minByOrNull { location.r2(it.location) }

        if (closest != null) {
            var destination = this.destination
            var min = Double.MAX_VALUE
            var max = Double.MIN_VALUE
            val points = possibleDestinations(location, closest).map { point ->
                val risk = risk(location, point, scans)
                min = minOf(risk, min)
                max = maxOf(risk, max)
                risk to point
            }.toList()
            val (_, possible) = points.minByOrNull { (risk, _) -> risk }!!
            if (destination == null
                || location.r2(destination) < sqr(TANK_SIZE / 2)
                || risk(location, destination, scans) * 0.8 > risk(location, possible, scans)
            ) {
                destination = possible
            }
            if (this.destination != destination) this.previous = location
            this.destination = destination

            Debug.onDraw(DebugKey.MinimumRiskMovement) {
                color = Color.BLUE
                fillCircle(destination, 8)
                previous?.let {
                    color = Color.RED
                    fillCircle(it, 8)
                }
                for ((risk, point) in points) {
                    val red = 255 * (risk - min) / (max - min)
                    color = Color(red.toInt(), 255 - red.toInt(), 0)
                    fillCircle(point, 8)
                }
            }

            return moveTo(location, velocity, destination)
        } else {
            return Polar(Angle.ZERO, 0.0)
        }
    }

    private fun possibleDestinations(location: Vector, closest: RobotScan) = sequence {
        val dist = sqrt(location.r2(closest.location))
        val min = (dist - 100).coerceIn(TANK_SIZE / 2, 200.0)
        val max = (dist + 100).coerceIn(200.0, 400.0)
        val diff = max - min

        val distanceSection = diff / distanceValues

        repeat(distanceValues) { distance ->
            repeat(angleValues) { angle ->
                yield(location + Polar(angleSection * angle.toDouble(), min + distanceSection * distance))
            }
        }
    }
        .filter { battleField.contains(it, 2 * TANK_SIZE / 3) }

    private fun risk(location: Vector.Cartesian, destination: Vector, enemies: List<RobotScan>): Double {
        var risk = 0.0
        val battleFieldWidth = battleField.width
        val battleFieldHeight = battleField.height
        val heading = theta(location.x, location.y, destination.x, destination.y)

        val enemyRisk = enemies.size - 1

        fun cornerRisk(risk: Int, x: Double, y: Double): Double {
            return risk / destination.r2(x, y)
        }

        // Corners are risky
        risk += cornerRisk(1, 0.0, 0.0)
        risk += cornerRisk(1, battleFieldWidth, 0.0)
        risk += cornerRisk(1, battleFieldWidth, battleFieldHeight)
        risk += cornerRisk(1, 0.0, battleFieldHeight)

        // Sides are more risky
        risk += cornerRisk(enemyRisk, battleFieldWidth / 2, 0.0)
        risk += cornerRisk(enemyRisk, battleFieldWidth, battleFieldHeight / 2)
        risk += cornerRisk(enemyRisk, battleFieldWidth / 2, battleFieldHeight)
        risk += cornerRisk(enemyRisk, 0.0, battleFieldHeight / 2)

        // Center is even more risky
        risk += cornerRisk(2 * enemyRisk, battleFieldWidth / 2, battleFieldHeight / 2)

        // Try not to go back to previous location
        previous?.let {
            risk += enemyRisk / destination.r2(it)
        }

        for (enemy in enemies) {
            var botRisk = enemy.energy
            botRisk *= 0.5 + abs(cos(theta(location.x, location.y, enemy.location) - heading))
            botRisk /= destination.r2(enemy.location)
            risk += botRisk
        }

        return risk
    }
}

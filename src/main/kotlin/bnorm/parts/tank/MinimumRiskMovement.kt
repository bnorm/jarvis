package bnorm.parts.tank

import bnorm.Cartesian
import bnorm.Polar
import bnorm.Vector
import bnorm.fillCircle
import bnorm.parts.contains
import bnorm.r2
import bnorm.robot.Robot
import bnorm.robot.RobotScan
import bnorm.sqr
import bnorm.theta
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

class MinimumRiskMovement(
    private val tank: Tank,
    private val aliveRobots: Collection<Robot>,
) : Movement {
    companion object {
        const val distanceValues = 3
        const val angleValues = 16
        const val angleSection = PI * 2 / angleValues
    }

    var destination: Vector.Cartesian? = null
    var previous: Vector.Cartesian? = null

    fun draw(g: Graphics2D) {
        destination?.let {
            g.color = Color.blue
            g.fillCircle(it, 8)
        }
        previous?.let {
            g.color = Color.red
            g.fillCircle(it, 8)
        }

        val location = Cartesian(tank.x, tank.y)
        val scans = aliveRobots.map { it.latest }
        val closest = scans.minByOrNull { location.r2(it.location) }
        if (closest != null) {
            var min = Double.MAX_VALUE
            var max = Double.MIN_VALUE
            val points = possibleDestinations(location, closest).map {
                val risk = risk(it, scans)
                min = minOf(risk, min)
                max = maxOf(risk, max)
                risk to it
            }.toList()
            for ((risk, point) in points) {
                val red = 255 * (risk - min) / (max - min)
                g.color = Color(red.toInt(), 255 - red.toInt(), 0)
                g.fillCircle(point, 8)
            }
        }
    }

    override fun setMove() {
        val location = Cartesian(tank.x, tank.y)
        val scans = aliveRobots.map { it.latest }
        val closest = scans.minByOrNull { location.r2(it.location) }

        if (closest != null) {
            var destination = this.destination
            val possible = possibleDestinations(location, closest).minByOrNull { risk(it, scans) }!!
            if (destination == null
                || r2(tank.x, tank.y, destination) < sqr(TANK_SIZE / 2)
                || risk(destination, scans) * 0.8 > risk(possible, scans)
            ) {
                destination = possible
            }
            tank.moveTo(destination)
            if (this.destination != destination) {
                this.previous = location
                this.destination = destination
            }
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
                yield(location + Polar(angleSection * angle, min + distanceSection * distance))
            }
        }
    }
        .filter { tank.battleField.contains(it, 2 * TANK_SIZE / 3) }

    private fun risk(destination: Vector, enemies: List<RobotScan>): Double {
        var risk = 0.0
        val battleFieldWidth = tank.battleField.width
        val battleFieldHeight = tank.battleField.height
        val heading = theta(tank.x, tank.y, destination.x, destination.y)

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
            botRisk *= 0.5 + abs(cos(theta(tank.x, tank.y, enemy.location) - heading))
            botRisk /= destination.r2(enemy.location)
            risk += botRisk
        }

        return risk
    }
}

package bnorm

import robocode.*
import robocode.util.Utils
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.*

val RADAR_SCAN_EXCESS = Rules.RADAR_TURN_RATE_RADIANS * 2 / 5

data class Enemy(
    val location: Vector,
    val velocity: Vector,
    val time: Long,
)

class Jarvis : AdvancedRobot() {
    private val enemyScans = mutableMapOf<String, ArrayDeque<Enemy>>()
    private var destination: Vector.Cartesian? = null

    override fun run() {
        setBodyColor(Color(0, 0, 0))
        setGunColor(Color(192, 0, 0))
        setRadarColor(Color(64, 64, 64))

        isAdjustRadarForGunTurn = true
        isAdjustGunForRobotTurn = true

        while (true) {
            if (enemyScans.isEmpty()) {
                setTurnRadarRightRadians(Double.MAX_VALUE)
                execute()
                continue
            }

            val location = Cartesian(x, y)
            val (name, enemy) = enemyScans.entries
                .map { (k, v) -> k to v.last() }
                .minByOrNull { (_, v) -> v.location.r2(x, y) }!!

            if (enemyScans.size > 1 || enemy.time < time) {
                setTurnRadarRightRadians(Double.MAX_VALUE)
            } else {
                val radarTurn = Utils.normalRelativeAngle(location.theta(enemy.location) - radarHeadingRadians)
                setTurnRadarRightRadians(sign(radarTurn) * RADAR_SCAN_EXCESS + radarTurn)
            }

            val bulletPower = 2.0
            val predicted = predictEnemyLocation(name, bulletPower)
            setTurnGunRightRadians(Utils.normalRelativeAngle(location.theta(predicted.location) - gunHeadingRadians))
            if (gunTurnRemainingRadians < Rules.GUN_TURN_RATE_RADIANS) setFire(bulletPower)

            minRiskMovement(location, enemy)
            execute()
        }
    }

    override fun onPaint(g: Graphics2D) {
        destination?.let { g.fillCircle(it, 8) }

        if (enemyScans.isEmpty()) return
        val (name, _) = enemyScans.entries
            .map { (k, v) -> k to v.last() }
            .minByOrNull { (_, v) -> v.location.r2(x, y) }!!

        predictEnemyLocations(name)
            .take(100)
            .forEach {
                g.fillCircle(it.location, 8)
            }
    }

    override fun onScannedRobot(e: ScannedRobotEvent) {
        val angle = headingRadians + e.bearingRadians
        val enemy = enemyScans[e.name] ?: ArrayDeque<Enemy>().also { enemyScans[e.name] = it }
        enemy += Enemy(
            location = Cartesian(x + sin(angle) * e.distance, y + cos(angle) * e.distance),
            velocity = Polar(e.headingRadians, e.velocity),
            time = e.time
        )
        while (enemy.size > 2) enemy.removeFirst()
    }

    override fun onRobotDeath(e: RobotDeathEvent) {
        enemyScans.remove(e.name)
    }

    private fun minRiskMovement(location: Vector, closest: Enemy) {
        var destination = this.destination

        val dangerous = enemyScans.values.map { it.last().location }
        val possible = possibleDestinations(closest).minByOrNull { risk(it, dangerous) }!!
        if (destination == null
            || location.r2(destination) < sqr(16.0)
            || risk(destination, dangerous) * .9 > risk(possible, dangerous)
        ) {
            destination = possible
        }

        moveTo(destination)
        this.destination = destination
    }

    private fun Vector.inBattleField(padding: Int = 0): Boolean {
        val widthPadding = width / 2 + padding
        val heightPadding = height / 2 + padding
        return x in widthPadding..battleFieldWidth - widthPadding &&
                y in heightPadding..battleFieldHeight - heightPadding
    }

    private fun possibleDestinations(closest: Enemy) = sequence {
        val location = Cartesian(x, y)

        val dist = sqrt(location.r2(closest.location))
        val min = minOf(200.0, maxOf(dist - 100, 0.0))
        val max = minOf(400.0, dist + 100)
        val diff = max - min

        val distanceValues = 3
        val angleValues = 16
        val angleSection = PI * 2 / angleValues
        val distanceSection = diff / distanceValues

        repeat(distanceValues) { distance ->
            repeat(angleValues) { angle ->
                yield(location + Polar(angleSection * angle, min + distanceSection * distance))
            }
        }
    }
        .filter { it.inBattleField(32) }

    private fun predictEnemyLocation(name: String, bulletPower: Double): Enemy {
        val enemyLocations = predictEnemyLocations(name)

        val x = x
        val y = y
        val bulletVelocity = Rules.getBulletSpeed(bulletPower)

        return enemyLocations.filterIndexed { index, predicted ->
            val bulletDistance = sqr(index * bulletVelocity)
            val enemyDistance = r2(x, y, predicted.location.x, predicted.location.y)
            bulletDistance > enemyDistance
        }.first()
    }

    private fun predictEnemyLocations(name: String): Sequence<Enemy> {
        val enemyScans = enemyScans[name]!!
        return sequence {
            var curr = enemyScans[enemyScans.size - 1]
            yield(curr)

            var prev = if (enemyScans.size < 2) curr
            else {
                val prev = enemyScans[enemyScans.size - 2]
                if (curr.time - prev.time > 1) curr
                else prev
            }

            while (true) {
                val acceleration = curr.velocity.r - prev.velocity.r
                val turn = curr.velocity.theta - prev.velocity.theta

                val r = truncate(-Rules.MAX_VELOCITY, curr.velocity.r + acceleration, Rules.MAX_VELOCITY)
                val velocity = Polar(curr.velocity.theta + turn, r)

                val next = curr.copy(
                    location = curr.location + curr.velocity,
                    velocity = velocity,
                )
                if (next.location.inBattleField()) {
                    yield(next)
                    prev = curr
                    curr = next
                } else {
                    break
                }
            }

            while (true) yield(curr)
        }
    }
}

private fun AdvancedRobot.risk(destination: Vector, enemies: List<Vector>): Double {
    var risk = 0.0
    val battleFieldWidth = battleFieldWidth
    val battleFieldHeight = battleFieldHeight
    val heading = theta(x, y, destination.x, destination.y)

    // Corners are risky
    risk += 10 / destination.r2(0.0, 0.0)
    risk += 10 / destination.r2(battleFieldWidth, 0.0)
    risk += 10 / destination.r2(battleFieldWidth, battleFieldHeight)
    risk += 10 / destination.r2(0.0, battleFieldHeight)

    // Center is very risky
    risk += enemies.size / destination.r2(battleFieldWidth / 2, battleFieldHeight / 2)

    for (enemy in enemies) {
        var botRisk = 100.0
        botRisk *= (1 + abs(cos(heading - theta(x, y, enemy.x, enemy.y))))
        botRisk /= destination.r2(enemy)
        risk += botRisk
    }

    return risk
}

fun AdvancedRobot.moveTo(destination: Vector.Cartesian) {
    var theta = Utils.normalRelativeAngle(theta(x, y, destination.x, destination.y) - headingRadians)
    var r = r(x, y, destination.x, destination.y)

    // Is it better to go backwards?
    if (theta > PI / 2) {
        theta -= PI
        r = -r
    } else if (theta < -PI / 2) {
        theta += PI
        r = -r
    }

    setTurnRightRadians(theta)
    setAhead(r)
}

fun Graphics2D.fillCircle(it: Vector, diameter: Int) {
    fillOval(it.x.toInt(), it.y.toInt(), diameter, diameter)
}

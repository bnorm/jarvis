package bnorm

import bnorm.parts.gun.Gun
import bnorm.parts.gun.CircularTargetingStrategy
import bnorm.parts.gun.DirectTargetingStrategy
import bnorm.parts.gun.LinearTargetingStrategy
import bnorm.parts.gun.VirtualGunService
import bnorm.parts.radar.AdaptiveRadarStrategy
import bnorm.parts.radar.Radar
import bnorm.robot.RobotScan
import bnorm.robot.RobotService
import robocode.AdvancedRobot
import robocode.RobotDeathEvent
import robocode.RoundEndedEvent
import robocode.Rules
import robocode.ScannedRobotEvent
import robocode.util.Utils
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Jarvis : AdvancedRobot() {
    private var destination: Vector? = null

    private val robotService = RobotService()

    private val gun = Gun(this)
    private val virtualGunService = VirtualGunService(
        gun, robotService, listOf(
            DirectTargetingStrategy(gun),
            LinearTargetingStrategy(gun),
            CircularTargetingStrategy(gun),
        )
    )

    private val radar = Radar(this)
    private val radarStrategy = AdaptiveRadarStrategy(radar, robotService.alive) {
        val robots = robotService.alive
        when {
            robots.size == 1 -> robots.first()
            // Gun is within 4 ticks of firing, target closest robot
            gunHeat - gunCoolingRate * 4 <= 0 -> robotService.closest(x, y)
            else -> null
        }
    }

    override fun run() {
        setBodyColor(Color(0x04, 0x04, 0x04))
        setGunColor(Color(0xF1, 0xF1, 0xF1))
        setRadarColor(Color(0x2D, 0x1E, 0x14))

        isAdjustRadarForGunTurn = true
        isAdjustGunForRobotTurn = true

        while (true) {
            radarStrategy.setMove()

            val target = robotService.closest(x, y)
            if (target != null) {
                val bulletPower = 2.0

                val predicted = virtualGunService.predict(target, bulletPower)
                setTurnGunRightRadians(Utils.normalRelativeAngle(predicted.theta - gunHeadingRadians))
                if (gunTurnRemainingRadians < Rules.GUN_TURN_RATE_RADIANS) {
                    setFire(bulletPower)
                    if (gunHeat <= gunCoolingRate) {
                        virtualGunService.fire(bulletPower)
                    }
                }

                minRiskMovement(target.history.latest)
            }

            execute()
        }
    }

    override fun onPaint(g: Graphics2D) {
        destination?.let { g.fillCircle(it, 8) }

        val time = time
        for ((name, holders) in virtualGunService.robots) {
            for (holder in holders) {
                when (holder.strategy) {
                    is DirectTargetingStrategy -> g.color = Color.red
                    is LinearTargetingStrategy -> g.color = Color.blue
                    is CircularTargetingStrategy -> g.color = Color.green
                    else -> g.color = Color.white
                }

                for (bullet in holder.virtualGun.bullets) {
                    g.drawLine(bullet.location(time - 1), bullet.location(time))
                }
            }
        }
    }

    override fun onScannedRobot(e: ScannedRobotEvent) {
        val scan = toRobotScan(e)
        robotService.onScan(e.name, scan)
        virtualGunService.scan(e.name, scan)
    }

    override fun onRobotDeath(e: RobotDeathEvent) {
        robotService.onDeath(e.name)
        virtualGunService.death(e.name)
    }

    override fun onRoundEnded(event: RoundEndedEvent) {
        robotService.onRoundEnd()
    }

    private fun minRiskMovement(closest: RobotScan) {
        var destination = this.destination

        val dangerous = robotService.alive.map { it.history.latest.location }
        val possible = possibleDestinations(closest).minByOrNull { risk(it, dangerous) }!!
        if (destination == null
            || r2(x, y, destination) < sqr(16.0)
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

    private fun possibleDestinations(closest: RobotScan) = sequence {
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

fun AdvancedRobot.toRobotScan(e: ScannedRobotEvent): RobotScan {
    val angle = headingRadians + e.bearingRadians
    return RobotScan(
        location = Cartesian(x + sin(angle) * e.distance, y + cos(angle) * e.distance),
        velocity = Polar(e.headingRadians, e.velocity),
        energy = e.energy,
        time = e.time,
        interpolated = false,
    )
}

fun AdvancedRobot.moveTo(destination: Vector) {
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

fun Graphics2D.drawLine(start: Vector, end: Vector) {
    drawLine(start.x.toInt(), start.y.toInt(), end.x.toInt(), end.y.toInt())
}

fun Graphics2D.fillCircle(it: Vector, diameter: Int) {
    fillOval(it.x.toInt(), it.y.toInt(), diameter, diameter)
}

package bnorm

import bnorm.kdtree.KdTree
import bnorm.parts.BattleField
import bnorm.parts.gun.CircularPrediction
import bnorm.parts.gun.DirectPrediction
import bnorm.parts.gun.GuessFactorPrediction
import bnorm.parts.gun.LinearPrediction
import bnorm.parts.gun.escapeAngle
import bnorm.parts.gun.robotAngle
import bnorm.parts.gun.virtual.RobotWaveManager
import bnorm.parts.gun.virtual.VirtualGuns
import bnorm.parts.gun.virtual.Wave
import bnorm.parts.gun.virtual.escapeAngle
import bnorm.parts.gun.virtual.radius
import bnorm.parts.radar.AdaptiveScan
import bnorm.parts.radar.Radar
import bnorm.parts.tank.MinimumRiskMovement
import bnorm.parts.tank.TANK_MAX_SPEED
import bnorm.parts.tank.Tank
import bnorm.robot.*
import com.jakewharton.picnic.RowDsl
import robocode.AdvancedRobot
import robocode.BulletHitEvent
import robocode.RobotDeathEvent
import robocode.RobotStatus
import robocode.RoundEndedEvent
import robocode.Rules
import robocode.ScannedRobotEvent
import robocode.SkippedTurnEvent
import robocode.StatusEvent
import robocode.util.Utils
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.*

data class WaveData<out T>(
    val scan: RobotScan,
    val snapshot: T,
    val cluster: List<Node<T>>,
) {
    data class Node<out T>(
        val value: T,
        val dist: Double
    )
}

class JarvisM : Jarvis(targeting = false)
class JarvisT : Jarvis(movement = false)

open class Jarvis @JvmOverloads constructor(
    private val targeting: Boolean = true,
    private val movement: Boolean = true,
) : AdvancedRobot() {


    companion object {
        private val dimensionScale = DoubleArray(RobotSnapshot.DIMENSIONS.size) { 1.0 }
        private val tree = KdTree(dimensionScale, RobotSnapshot::guessFactorDimensions)

        private val robotService = RobotService { robot ->
            robot.install(RobotSnapshots) {
                factory = RobotSnapshots.Factory { scan, prevSnapshot ->
                    robotSnapshot(scan, prevSnapshot)
                }
            }

            robot.install(VirtualGuns) {
                predictions = listOf(
                    DirectPrediction(self),
                    LinearPrediction(self),
                    CircularPrediction(self),
                    GuessFactorPrediction(self, tree) { it.context[RobotSnapshots].latest }
                )
            }
        }
    }

    private var battleField: BattleField? = null

    private val waveServices = mutableMapOf<String, RobotWaveManager<WaveData<RobotSnapshot>>>()

    override fun run() {
        setBodyColor(Color(0x04, 0x04, 0x04))
        setGunColor(Color(0xF1, 0xF1, 0xF1))
        setRadarColor(Color(0x2D, 0x1E, 0x14))

        isAdjustRadarForGunTurn = true
        isAdjustGunForRobotTurn = true

        val tank = Tank(this@Jarvis)
        val movementStrategy = MinimumRiskMovement(tank, robotService.alive)

        val radar = Radar(this@Jarvis)
        val radarStrategy = AdaptiveScan(radar, robotService.alive) {
            val robots = robotService.alive
            when {
                robots.size == 1 -> robots.first()
                // Gun is within 4 ticks of firing, target closest robot
                gunHeat - gunCoolingRate * 4 <= 0 -> robotService.closest(x, y)
                else -> null
            }
        }

        while (true) {
            if (movement) {
                movementStrategy.setMove()
            }
            radarStrategy.setMove()

            if (targeting) {
                val target = robotService.closest(x, y)
                if (target != null) {
                    val power = minOf(3.0, energy)

                    val predicted = target.context[VirtualGuns].fire(power)
                    setTurnGunRightRadians(Utils.normalRelativeAngle(predicted.theta - gunHeadingRadians))

                    if (abs(gunTurnRemainingRadians) < (PI / 360)) {
                        setFireBullet(power)
                    }

                    val wave = target.context[VirtualGuns]
                        .prediction<GuessFactorPrediction<RobotSnapshot>>()!!
                        .latestWave

                    waveServices.getOrPut(target.name) {
                        RobotWaveManager<WaveData<RobotSnapshot>>(robotService.self, target).apply {
                            listen { wave ->
                                val waveSnapshot = wave.value.snapshot
                                waveSnapshot.guessFactor = wave.guessFactor(target.latest)
                                tree.add(waveSnapshot)
                                trainDimensionScales(wave)
                            }
                        }
                    }.fire(power, wave)
                }
            }

            execute()
        }
    }

    override fun onSkippedTurn(event: SkippedTurnEvent) {
//        tree.print()
        println("SKIPPED! ${event.skippedTurn} ${time}")
    }

    override fun onPaint(g: Graphics2D) {
        val time = time

        for (robot in robotService.alive) {
            val virtualGuns = robot.context[VirtualGuns]
            for (gun in virtualGuns.guns) {
                g.draw(gun, time)
            }
        }

        for (waveService in waveServices.values) {
            for (wave in waveService.waves) {
                g.drawWave(robotService.self.latest, wave, time)
            }
        }

//        val target = robotService.closest(x, y)
//        if (target != null) {
//            val snapshot = target.latest.toGuessFactorPoint(0.0)
//            val neighbors = tree.neighbors(snapshot).take(100).toList()
////            println("snapshot=$snapshot neighbors=${neighbors.joinToString { "(${it.distSqr},${it.value})" }}")
//            g.drawCluster(target.latest, neighbors)
//        }
    }

    override fun onStatus(e: StatusEvent) {
        val battleField = battleField ?: BattleField(battleFieldWidth, battleFieldHeight).also { battleField = it }
        robotService.onStatus(name, e.status.toRobotScan(), battleField)
    }

    override fun onScannedRobot(e: ScannedRobotEvent) {
        val bulletHitEvent = tookDamage.remove(e.name)
        val scan = e.toRobotScan(bulletHitEvent?.damage ?: 0.0)
        robotService.onScan(e.name, scan, battleField!!)
    }

    override fun onRobotDeath(e: RobotDeathEvent) {
        robotService.onKill(e.name)
    }

    override fun onRoundEnded(event: RoundEndedEvent) {
        robotService.onRoundEnd()
    }

    private val tookDamage = mutableMapOf<String, BulletHitEvent>()
    override fun onBulletHit(event: BulletHitEvent) {
        tookDamage[event.name] = event
    }

    private fun ScannedRobotEvent.toRobotScan(damage: Double = 0.0): RobotScan {
        val angle = this@Jarvis.headingRadians + bearingRadians
        return RobotScan(
            location = Cartesian(x + sin(angle) * distance, y + cos(angle) * distance),
            velocity = Polar(headingRadians, velocity),
            energy = energy,
            damage = damage,
            time = time,
            interpolated = false,
        )
    }

    private fun RobotStatus.toRobotScan(): RobotScan {
        return RobotScan(
            location = Cartesian(x, y),
            velocity = Polar(headingRadians, velocity),
            energy = energy,
            damage = 0.0,
            time = time,
            interpolated = false,
        )
    }

    private fun trainDimensionScales(
        wave: Wave<WaveData<RobotSnapshot>>
    ) {
        val distance = wave.radius(time)
        val snapshot = wave.value.snapshot

        val delta = robotAngle(distance) / escapeAngle(TANK_MAX_SPEED)
        val gf = snapshot.guessFactor

        var t = 0
        val tMeans = DoubleArray(dimensionScale.size)
        val tVariances = DoubleArray(dimensionScale.size)

        var g = 0
        val gMeans = DoubleArray(dimensionScale.size)
        val gVariances = DoubleArray(dimensionScale.size)

        var b = 0
        val bMeans = DoubleArray(dimensionScale.size)
        val bVariances = DoubleArray(dimensionScale.size)

        for (neighbor in wave.value.cluster) {
            val dimensions = neighbor.value.guessFactorDimensions
            rollingVariance(++t, tMeans, tVariances, dimensions)

            if (abs(gf - neighbor.value.guessFactor) < delta) {
                rollingVariance(++g, gMeans, gVariances, dimensions)
            } else {
                rollingVariance(++b, bMeans, bVariances, dimensions)
            }
        }

        // if 5% of predictions were good...
        if (g >= t / 20) {
            for (i in dimensionScale.indices) {
                if (gVariances[i] > 0.0) {
                    val scale = dimensionScale[i]
                    val weight = 2.0 / (1.0 + scale)

                    val gDist = sqr(gMeans[i] - snapshot.guessFactorDimensions[i])
                    val bDist = sqr(bMeans[i] - snapshot.guessFactorDimensions[i])

                    if (gDist < bDist && gDist < gVariances[i] && gVariances[i] < bVariances[i]) {
                        dimensionScale[i] = (scale + weight * 0.01)
                    }
                }
            }

            val sum = dimensionScale.sum()
            for (i in dimensionScale.indices) {
                dimensionScale[i] = (dimensionScale[i] * dimensionScale.size / sum)
            }
        }

//        println(table {
//            cellStyle { border = true }
//            row { cell("Dimension") { columnSpan = 3 }; RobotSnapshot.DIMENSIONS.forEach { cell(it.name) } }
//            row { cell("Scale") { columnSpan = 3 }; dimension(dimensionScale) }
//            row { cell("Target") { columnSpan = 3 }; dimension(snapshot.guessFactorDimensions) }
//            row { cell("Mean") { rowSpan = 3 }; cell("Good"); cell(g); dimension(gMeans) }
//            row { cell("Bad"); cell(b); dimension(bMeans) }
//            row { cell("All"); cell(t); dimension(tMeans) }
//            row { cell("Variance") { rowSpan = 3 }; cell("Good"); cell(g); dimension(gVariances) }
//            row { cell("Bad"); cell(b); dimension(bVariances) }
//            row { cell("All"); cell(t); dimension(tVariances) }
//        })
//        println()
    }
}

private fun RowDsl.dimension(values: DoubleArray) {
    for (v in values) {
        cell((v * 1000).toInt() / 1000.0)
    }
}

fun <T : Comparable<T>> Iterable<T>.increasing(): Boolean {
    val iterator = iterator()
    if (!iterator.hasNext()) return true
    var prev = iterator.next()
    while (iterator.hasNext()) {
        val next = iterator.next()
        if (prev > next) return false
        prev = next
    }
    return true
}

fun <T, C : Comparable<C>> Iterable<T>.increasingBy(selector: (T) -> C): Boolean {
    val iterator = iterator()
    if (!iterator.hasNext()) return true
    var prev = iterator.next()
    while (iterator.hasNext()) {
        val next = iterator.next()
        if (selector(prev) > selector(next)) return false
        prev = next
    }
    return true
}

val BulletHitEvent.damage: Double get() = Rules.getBulletDamage(bullet.power)

fun Wave<WaveData<RobotSnapshot>>.guessFactor(end: RobotScan): Double {
    val waveHeading = origin.theta(value.scan.location)

    val waveBearing = Utils.normalRelativeAngle(origin.theta(end.location) - waveHeading)
    val escapeAngle = escapeAngle(TANK_MAX_SPEED)

    val direction = value.snapshot.rotateDirection
    return (direction * waveBearing / escapeAngle).coerceIn(-1.0, 1.0)
}

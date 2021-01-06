package bnorm

import bnorm.kdtree.KdTree
import bnorm.neural.Activation
import bnorm.neural.NeuralNetwork
import bnorm.parts.BattleField
import bnorm.parts.gun.AntiGuessFactorPrediction
import bnorm.parts.gun.CircularPrediction
import bnorm.parts.gun.DirectPrediction
import bnorm.parts.gun.GuessFactorPrediction
import bnorm.parts.gun.LinearPrediction
import bnorm.parts.gun.NeuralGuessFactorPrediction
import bnorm.parts.gun.virtual.VirtualGuns
import bnorm.parts.gun.virtual.VirtualWaves
import bnorm.parts.gun.virtual.Wave
import bnorm.parts.gun.virtual.escapeAngle
import bnorm.parts.radar.AdaptiveScan
import bnorm.parts.radar.Radar
import bnorm.parts.tank.MinimumRiskMovement
import bnorm.parts.tank.TANK_MAX_SPEED
import bnorm.parts.tank.Tank
import bnorm.parts.tank.orbit
import bnorm.robot.*
import com.jakewharton.picnic.RowDsl
import com.jakewharton.picnic.table
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
import kotlin.random.Random
import kotlin.time.measureTime

data class WaveData<out T>(
    val scan: RobotScan,
    val snapshot: T,
    val cluster: List<Node<T>>,
    var real: Boolean = false,
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
        object RobotSnapshotWaves : VirtualWaves.Feature<WaveData<RobotSnapshot>>()

        private val robotService = RobotService { robot ->

            val virtualTreeScales = DoubleArray(RobotSnapshot.DIMENSIONS.size) { 1.0 }
            val virtualTree =
                KdTree(virtualTreeScales, KdTree.DEFAULT_BUCKET_SIZE, RobotSnapshot::guessFactorDimensions)

            val realTreeScales = DoubleArray(RobotSnapshot.DIMENSIONS.size) { 1.0 }
            val realTree = KdTree(virtualTreeScales, KdTree.DEFAULT_BUCKET_SIZE, RobotSnapshot::guessFactorDimensions)

            val gfTree = KdTree<RobotSnapshot>(doubleArrayOf(1.0), KdTree.DEFAULT_BUCKET_SIZE) {
                doubleArrayOf(it.guessFactor)
            }
            val neuralNetwork = NeuralNetwork(
                2 * RobotSnapshot.DIMENSIONS.size + 1, 2 * 31, 2 * 31, 31,
                activation = Activation.Sigmoid, biased = true,
            )

            val virtualGf = GuessFactorPrediction(self, virtualTree) { it.context[RobotSnapshots].latest }
            val realGf = GuessFactorPrediction(self, realTree) { it.context[RobotSnapshots].latest }
            val neuralGf = NeuralGuessFactorPrediction(self, neuralNetwork, virtualGf, RobotSnapshot::guessFactorDimensions)

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
                    virtualGf,
                    realGf,
//                    AntiGuessFactorPrediction(self, virtualGf, realGf),
                    neuralGf,
                )
            }

            robot.install(RobotSnapshotWaves) {
                listen { wave ->
                    val waveSnapshot = wave.value.snapshot
                    val guessFactor = wave.guessFactor(robot.latest)
                    waveSnapshot.guessFactor = guessFactor
                    if (wave.value.real) {
                        realTree.add(waveSnapshot)
                        trainDimensionScales(realTreeScales, wave, robot.latest.time)
                    } else {
                        virtualTree.add(waveSnapshot)
                        trainDimensionScales(virtualTreeScales, wave, robot.latest.time)

//                        val neighbors = gfTree.neighbors(waveSnapshot, 100)
//                        gfTree.add(waveSnapshot)

//                        println("Common GF")
//                        printSnapshots(waveSnapshot, neighbors.map { it.value })
//
//                        println("Common Snapshots")
//                        printSnapshots(waveSnapshot, wave.value.cluster.map { it.value })

//                        for (neighbor in neighbors) {
//                            neuralGf.train(waveSnapshot, neighbor.value, guessFactor)
//                        }
                        println("training " + measureTime {
                            neuralGf.train(waveSnapshot, wave.value.cluster.map { it.value }, guessFactor)
                        })
                    }
                }
            }
        }
    }

    private var battleField: BattleField? = null

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
                    // Only fire if the newly predicted angle is close to the old predicted angle
                    val bullet = if (abs(gunTurnRemainingRadians) <= (PI / 360)) setFireBullet(power) else null

                    val guessFactor = target.context[VirtualGuns]
                        .prediction<GuessFactorPrediction<RobotSnapshot>>()
                    val wave = guessFactor[0].latestWave
                    target.context[RobotSnapshotWaves].fire(power, wave)
                    if (bullet != null) {
                        val wave = guessFactor[1].latestWave
                        wave.real = true
                        target.context[RobotSnapshotWaves].fire(power, wave)
                    }
                }
            }

            execute()
        }
    }

    override fun onSkippedTurn(event: SkippedTurnEvent) {
        println("SKIPPED! ${event.skippedTurn} ${time}")
    }

    override fun onPaint(g: Graphics2D) {
//        val self = robotService.self
//        val target = robotService.closest(self.latest.location.x, self.latest.location.y)
//        if (target != null) {
//            g.color = Color.green
//            self.orbit(target, 500.0, Double.POSITIVE_INFINITY)
//                .take(50)
//                .forEach {
//                    g.drawCircle(it, 2.0)
//                }
//            g.color = Color.red
//            self.orbit(target, 500.0, Double.NEGATIVE_INFINITY)
//                .take(50)
//                .forEach {
//                    g.drawCircle(it, 2.0)
//                }
//        }

        val time = time

        for (robot in robotService.alive) {
            val virtualGuns = robot.context[VirtualGuns]
            for ((index, gun) in virtualGuns.guns.withIndex()) {
                g.drawBullets(gun, time)
                g.drawSuccess(index, gun)
            }

//            val virtualWaves = robot.context[RobotSnapshotWaves]
//            for (wave in virtualWaves.waves) {
//                g.drawWave(robotService.self.latest, wave, time)
//            }
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
}


private fun printSnapshots(actual: RobotSnapshot, cluster: List<RobotSnapshot>) {
    if (cluster.isEmpty()) return

    val dimensions = RobotSnapshot.DIMENSIONS.size

    val values = Array(dimensions) { DoubleArray(cluster.size) }

    var n = 0
    val sums = DoubleArray(dimensions)
    val mins = DoubleArray(dimensions)
    val maxs = DoubleArray(dimensions)
    val means = DoubleArray(dimensions)
    val variances = DoubleArray(dimensions)

    for (neighbor in cluster) {
        for (d in 0 until dimensions) {
            values[d][n] = neighbor.guessFactorDimensions[d]
            sums[d] += neighbor.guessFactorDimensions[d]
            mins[d] = minOf(neighbor.guessFactorDimensions[d], mins[d])
            maxs[d] = maxOf(neighbor.guessFactorDimensions[d], maxs[d])
        }
        rollingVariance(++n, means, variances, neighbor.guessFactorDimensions)
    }

    val medians = DoubleArray(dimensions)
    for ((d, value) in values.withIndex()) {
        value.sort()
        medians[d] = value[value.size / 2]
    }

    val stddev = DoubleArray(dimensions)
    for ((d, variance) in variances.withIndex()) {
        stddev[d] = sqrt(variance)
    }

    val goodBad = (0 until dimensions)
        .map { d -> if (stddev[d] < 0.2 && abs(actual.guessFactorDimensions[d] - medians[d]) < stddev[d]) "Y" else "N" }

//    if (goodBad.any { it == "Y" }) {
        println(table {
            cellStyle { border = true }
            row { cell("Dimensions"); RobotSnapshot.DIMENSIONS.forEach { cell(it.name) } }
            row { cell("Actual"); dimension(actual.guessFactorDimensions) }
            row { cell("Min"); dimension(mins) }
            row { cell("Max"); dimension(maxs) }
            row { cell("Median"); dimension(medians) }
            row { cell("Mean"); dimension(means) }
            row { cell("Variance"); dimension(variances) }
            row { cell("StdDev"); dimension(stddev) }
            row {
                cell("Good?")
                goodBad.forEach { cell(it) }
            }
        })
        println()
//    }
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


private fun trainDimensionScales(
    dimensionScale: DoubleArray,
    wave: Wave<WaveData<RobotSnapshot>>,
    time: Long
) {
//    val distance = wave.radius(time)
//    val snapshot = wave.value.snapshot
//
//    val delta = robotAngle(distance) / escapeAngle(TANK_MAX_SPEED)
//    val gf = snapshot.guessFactor
//
//    var t = 0
//    val tMeans = DoubleArray(dimensionScale.size)
//    val tVariances = DoubleArray(dimensionScale.size)
//
//    var g = 0
//    val gMeans = DoubleArray(dimensionScale.size)
//    val gVariances = DoubleArray(dimensionScale.size)
//
//    var b = 0
//    val bMeans = DoubleArray(dimensionScale.size)
//    val bVariances = DoubleArray(dimensionScale.size)
//
//    for (neighbor in wave.value.cluster) {
//        val dimensions = neighbor.value.guessFactorDimensions
//        rollingVariance(++t, tMeans, tVariances, dimensions)
//
//        if (abs(gf - neighbor.value.guessFactor) < delta) {
//            rollingVariance(++g, gMeans, gVariances, dimensions)
//        } else {
//            rollingVariance(++b, bMeans, bVariances, dimensions)
//        }
//    }
//
//    // if 5% of predictions were good...
//    if (g >= t / 20) {
//        for (i in dimensionScale.indices) {
//            if (gVariances[i] > 0.0) {
//                val scale = dimensionScale[i]
//                val weight = 2.0 / (1.0 + scale)
//
//                val gDist = sqr(gMeans[i] - snapshot.guessFactorDimensions[i])
//                val bDist = sqr(bMeans[i] - snapshot.guessFactorDimensions[i])
//
//                if (gDist < bDist && gDist < gVariances[i] && gVariances[i] < bVariances[i]) {
//                    dimensionScale[i] = (scale + weight * 0.01)
//                }
//            }
//        }
//
//        val sum = dimensionScale.sum()
//        for (i in dimensionScale.indices) {
//            dimensionScale[i] = (dimensionScale[i] * dimensionScale.size / sum)
//        }
//    }

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

private fun RowDsl.dimension(values: DoubleArray) {
    for (v in values) {
        cell((v * 1000).toInt() / 1000.0)
    }
}

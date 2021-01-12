package bnorm

import bnorm.coroutines.CoroutineRobot
import bnorm.coroutines.RobotTurn
import bnorm.kdtree.KdTree
import bnorm.neural.Activation
import bnorm.neural.NeuralNetwork
import bnorm.parts.BattleField
import bnorm.parts.gun.CircularPrediction
import bnorm.parts.gun.DirectPrediction
import bnorm.parts.gun.GuessFactorPrediction
import bnorm.parts.gun.LinearPrediction
import bnorm.parts.gun.virtual.VirtualGuns
import bnorm.parts.gun.virtual.VirtualWaves
import bnorm.parts.gun.virtual.Wave
import bnorm.parts.gun.virtual.WaveContext
import bnorm.parts.gun.virtual.escapeAngle
import bnorm.parts.gun.virtual.guns
import bnorm.parts.gun.virtual.waves
import bnorm.parts.radar.AdaptiveScan
import bnorm.parts.radar.Radar
import bnorm.parts.tank.MinimumRiskMovement
import bnorm.parts.tank.Movement
import bnorm.robot.*
import com.jakewharton.picnic.BorderStyle
import com.jakewharton.picnic.RowDsl
import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import robocode.Bullet
import robocode.BulletHitEvent
import robocode.RobotDeathEvent
import robocode.RoundEndedEvent
import robocode.Rules
import robocode.ScannedRobotEvent
import robocode.SkippedTurnEvent
import robocode.StatusEvent
import robocode.util.Utils
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.*

// Movement only version of Jarvis
class JarvisM : Jarvis(targetingEnabled = false)

// Targeting Only version of Jarvis
class JarvisT : Jarvis(movementEnabled = false)

open class Jarvis @JvmOverloads constructor(
    private val targetingEnabled: Boolean = true,
    private val movementEnabled: Boolean = true,
) : CoroutineRobot() {
    companion object {
        object RealBullet : WaveContext.Feature<Boolean>
        object VirtualCluster : WaveContext.Feature<Collection<KdTree.Neighbor<RobotSnapshot>>>
        object RealCluster : WaveContext.Feature<Collection<KdTree.Neighbor<RobotSnapshot>>>

        private val robotService = RobotService { robot ->
            val virtualTreeScales = DoubleArray(RobotSnapshot.DIMENSIONS.size) { 1.0 }
            val virtualTree =
                KdTree(virtualTreeScales, KdTree.DEFAULT_BUCKET_SIZE, RobotSnapshot::guessFactorDimensions)

            val realTreeScales = DoubleArray(RobotSnapshot.DIMENSIONS.size) { 1.0 }
            val realTree =
                KdTree(virtualTreeScales, KdTree.DEFAULT_BUCKET_SIZE, RobotSnapshot::guessFactorDimensions)

            val gfTree = KdTree<RobotSnapshot>(doubleArrayOf(1.0), KdTree.DEFAULT_BUCKET_SIZE) {
                doubleArrayOf(it.guessFactor)
            }
            val neuralNetwork = NeuralNetwork(
                2 * RobotSnapshot.DIMENSIONS.size + 1, 2 * 31, 2 * 31, 31,
                activation = Activation.Sigmoid, biased = true,
            )

            val virtualCluster by robot.memorized {
                virtualTree.neighbors(robot.snapshot, 100)
            }
            val realCluster by robot.memorized {
                realTree.neighbors(robot.snapshot, 100)
            }

            //            val neuralGf =
//                NeuralGuessFactorPrediction(self, neuralNetwork, virtualGf, RobotSnapshot::guessFactorDimensions)

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
                    GuessFactorPrediction(self) { virtualCluster },
                    GuessFactorPrediction(self) { realCluster },
                    // AntiGuessFactorPrediction(self, { virtualCluster }, { realCluster }),
                    // neuralGf,
                )
            }

            robot.install(VirtualWaves) {
                onWave = {
                    context[RobotSnapshots] = robot.snapshot
                    context[VirtualCluster] = virtualCluster
                    if (context[RealBullet]) {
                        context[RealCluster] = realCluster
                    }
                    context[EscapeAngle] = escapeAngle(self, robot, speed)
                }
                listen { wave ->
                    coroutineScope {
                        val snapshot = wave.snapshot
                        snapshot.guessFactor = wave.guessFactor(robot.latest)

                        if (wave.context[RealBullet]) {
                            launch {
                                realTree.add(snapshot)
                                trainDimensionScales(realTreeScales, wave, robot.latest.time)
                            }
                        }
                        launch {
                            virtualTree.add(snapshot)
                            trainDimensionScales(virtualTreeScales, wave, robot.latest.time)
                        }
                    }
                }
            }
        }
    }

    override suspend fun init(): RobotTurn {
        val battleField = BattleField(battleFieldWidth, battleFieldHeight)
        val movement = MinimumRiskMovement(battleField, robotService.alive)

        val radar = Radar(this)
        val radarStrategy = AdaptiveScan(radar, robotService.alive) {
            val robots = robotService.alive
            when {
                robots.size == 1 -> robots.first()
                // Gun is within 4 ticks of firing, target closest robot
                gunHeat - gunCoolingRate * 4 <= 0 -> robotService.closest()
                else -> null
            }
        }

        setBodyColor(Color(0x04, 0x04, 0x04))
        setGunColor(Color(0xF1, 0xF1, 0xF1))
        setRadarColor(Color(0x2D, 0x1E, 0x14))

        isAdjustRadarForGunTurn = true
        isAdjustGunForRobotTurn = true

        return { events ->
            coroutineScope {
                val tookDamage = mutableMapOf<String, BulletHitEvent>()
                events.collect { e ->
                    when (e) {
                        is BulletHitEvent -> tookDamage[e.name] = e
                        is RobotDeathEvent -> robotService.onKill(e.name)
                        is RoundEndedEvent -> robotService.onRoundEnd()
                        is StatusEvent -> {
                            robotService.onStatus(name, e.toRobotScan(), battleField)
                        }
                        is ScannedRobotEvent -> launch(Computation) {
                            val damage = tookDamage.remove(e.name)?.damage ?: 0.0
                            robotService.onScan(e.name, e.toRobotScan(damage), battleField)
                        }
                    }
                }
            }

            coroutineScope {
                radarStrategy.setMove()

                if (movementEnabled) {
                    launch(Computation) { movement.move() }
                }

                if (targetingEnabled) {
                    launch(Computation) {
                        val target = robotService.closest()
                        if (target != null) {
                            val power = minOf(3.0, energy)
                            val bullet = fire(target.guns.best.predict(target, power), power)
                            if (bullet != null) target.guns.fire(power)
                            target.waves.fire(power) {
                                context[RealBullet] = bullet != null
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun fire(predicted: Vector, power: Double): Bullet? {
        return withContext(Main) {
            setTurnGunRightRadians(Utils.normalRelativeAngle(predicted.theta - gunHeadingRadians))
            if (abs(gunTurnRemainingRadians) <= (PI / 360)) setFireBullet(power) else null
        }
    }

    suspend fun Movement.move() {
        val latest = robotService.self.latest
        val move = invoke(latest.location, latest.velocity)
        withContext(Main) {
            setTurnRightRadians(move.theta)
            setAhead(move.r)
        }
    }

    override fun onSkippedTurn(event: SkippedTurnEvent) {
        println("SKIPPED! ${event.skippedTurn} ${time}")
    }

    override fun onRoundEnded(event: RoundEndedEvent) {
        super.onRoundEnded(event)
        onPrint()
    }

    private fun onPrint() {
        println(table {
            style {
                borderStyle = BorderStyle.Hidden
            }
            cellStyle {
                paddingLeft = 1
                paddingRight = 1
            }
            for (robot in robotService.all) {
                row {
                    cellStyle {
                        alignment = TextAlignment.BottomCenter
                        borderBottom = true
                    }
                    cell(robot.name) { columnSpan = 5 }
                }
                row {
                    cellStyle {
                        alignment = TextAlignment.BottomCenter
                        borderBottom = true
                    }
                    cell("Virtual Gun") { borderRight = true }
                    cell("Score")
                    cell("Hit")
                    cell("Fired")
                    cell("Success")
                }
                val virtualGuns = robot.guns
                for (gun in virtualGuns.guns.sortedBy { -it.success }) {
                    row {
                        cellStyle { alignment = TextAlignment.MiddleRight }
                        cell(gun.prediction::class.simpleName) {
                            alignment = TextAlignment.MiddleLeft
                            borderRight = true
                        }
                        cell(String.format("%.3f", gun.success))
                        cell(String.format("%d", gun.hit))
                        cell(String.format("%d", gun.fired))
                        cell(String.format("%.1f", gun.hit * 100.0 / gun.fired))
                    }
                }
            }
        })
    }

    override fun onPaint(g: Graphics2D) = runBlocking<Unit> {
        val time = time
        val target = robotService.closest()

        if (target != null) {
//            drawPath(g, self, target)

//            g.draw(target.snapshot.wallProbe)
        }

        for (robot in robotService.alive) {
            val virtualGuns = robot.guns
            for ((index, gun) in virtualGuns.guns.withIndex()) {
                g.drawBullets(gun, time)
                g.drawSuccess(index, gun)
            }

            val virtualWaves = robot.waves
            for (wave in virtualWaves.waves) {
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

    private fun StatusEvent.toRobotScan(): RobotScan {
        return RobotScan(
            location = Cartesian(status.x, status.y),
            velocity = Polar(status.headingRadians, status.velocity),
            energy = status.energy,
            damage = 0.0,
            time = status.time,
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

val BulletHitEvent.damage: Double get() = Rules.getBulletDamage(bullet.power)

fun Wave.guessFactor(end: RobotScan): Double {
    val waveHeading = origin.theta(snapshot.scan.location)
    val direction = snapshot.rotateDirection
    val waveBearing = direction * Utils.normalRelativeAngle(origin.theta(end.location) - waveHeading)
    val escapeAngle = if (direction < 0) escapeAngle.reverse else escapeAngle.forward
    return (waveBearing / escapeAngle).coerceIn(-1.0, 1.0)
}


private fun trainDimensionScales(
    dimensionScale: DoubleArray,
    wave: Wave,
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

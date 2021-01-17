package bnorm

import bnorm.coroutines.CoroutineRobot
import bnorm.coroutines.RobotTurn
import bnorm.kdtree.KdTree
import bnorm.neural.NeuralNetwork
import bnorm.parts.BattleField
import bnorm.parts.gun.CircularPrediction
import bnorm.parts.gun.DirectPrediction
import bnorm.parts.gun.GuessFactorPrediction
import bnorm.parts.gun.LinearPrediction
import bnorm.parts.gun.robotAngle
import bnorm.parts.gun.virtual.VirtualGuns
import bnorm.parts.gun.virtual.VirtualWaves
import bnorm.parts.gun.virtual.Wave
import bnorm.parts.gun.virtual.WaveContext
import bnorm.parts.gun.virtual.guns
import bnorm.parts.gun.virtual.radius
import bnorm.parts.gun.virtual.waves
import bnorm.parts.radar.AdaptiveScan
import bnorm.parts.radar.Radar
import bnorm.parts.tank.MinimumRiskMovement
import bnorm.parts.tank.Movement
import bnorm.parts.tank.TANK_MAX_SPEED
import bnorm.parts.tank.escape.EscapeAngle
import bnorm.parts.tank.escape.escape
import bnorm.parts.tank.escape.escapeAngle
import bnorm.parts.tank.escape.escapeCircle
import bnorm.robot.*
import com.jakewharton.picnic.BorderStyle
import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
            val virtualTreeNetwork = NeuralNetwork(RobotSnapshot.DIMENSIONS.size, 1) { _, _ -> 1.0 }
            val virtualTree = KdTree(
                virtualTreeNetwork.layers[0].weights,
                KdTree.DEFAULT_BUCKET_SIZE,
                RobotSnapshot::guessFactorDimensions
            )

            val realTreeNetwork = NeuralNetwork(RobotSnapshot.DIMENSIONS.size, 1) { _, _ -> 1.0 }
            val realTree = KdTree(
                realTreeNetwork.layers[0].weights,
                KdTree.DEFAULT_BUCKET_SIZE,
                RobotSnapshot::guessFactorDimensions
            )

//            val gfTree = KdTree<RobotSnapshot>(doubleArrayOf(1.0), KdTree.DEFAULT_BUCKET_SIZE) {
//                doubleArrayOf(it.guessFactor)
//            }
//            val neuralNetwork = NeuralNetwork(2 * RobotSnapshot.DIMENSIONS.size + 1, 2 * 31, 31, biased = true)

            val virtualCluster by robot.memorized {
                trace("neighbors-virtual") { virtualTree.neighbors(robot.snapshot, 50) }
            }
            val realCluster by robot.memorized {
                trace("neighbors-real") { realTree.neighbors(robot.snapshot, 50) }
            }
            val cluster by robot.memorized {
                trace("neighbors") { realCluster + virtualCluster }
            }

//            val neuralGf = NeuralGuessFactorPrediction(
//                self,
//                robot,
//                neuralNetwork,
//                { it.snapshot to virtualCluster },
//                RobotSnapshot::guessFactorDimensions
//            )

            robot.install(VirtualGuns) {
                predictions = mapOf(
                    "GF" to GuessFactorPrediction(self, robot) { cluster },
                    "Virtual GF" to GuessFactorPrediction(self, robot) { realCluster },
                    "Real GF" to GuessFactorPrediction(self, robot) { virtualCluster },
                    "Circular" to CircularPrediction(self, robot),
                    "Linear" to LinearPrediction(self, robot),
                    "Direct" to DirectPrediction(self, robot),
                )
            }

            robot.install(VirtualWaves) {
                onWave = {
                    context[RobotSnapshots] = robot.snapshot
                    context[VirtualCluster] = virtualCluster
                    if (context[RealBullet]) {
                        context[RealCluster] = realCluster
                    }
                    context[EscapeAngle] = self.battleField.escape(self.latest.location, robot.latest.location, speed)
                }
                listen { wave ->
//                    coroutineScope {
                    val snapshot = wave.snapshot
                    snapshot.guessFactor = wave.guessFactor(robot.latest)

                    if (wave.context[RealBullet]) {
//                            launch {
                        realTree.add(snapshot)
//                                trainDimensionScales(
//                                    realTreeNetwork,
//                                    wave,
//                                    wave.context[RealCluster],
//                                    robot.latest.time
//                                )
//                            }
                    }
//                        launch {
                    virtualTree.add(snapshot)
//                            trainDimensionScales(
//                                virtualTreeNetwork,
//                                wave,
//                                wave.context[VirtualCluster],
//                                robot.latest.time
//                            )
//                        }
//                        launch {
//                            neuralGf.train(snapshot, wave.context[VirtualCluster])
//                        }

                    // TODO save snapshot to file
//                        snapshotChannel.send(robot to snapshot)
//                    }
                }
            }

            robot.install(RobotSnapshots) {
                factory = RobotSnapshots.Factory { scan, prevSnapshot ->
                    robotSnapshot(scan, prevSnapshot, robot.waves.waves.size.toLong())
                }
            }
        }

        val snapshotChannel = Channel<Pair<Robot, RobotSnapshot>>(Channel.UNLIMITED)
    }

    override suspend fun init(): RobotTurn {
        GlobalScope.launch(Computation) {
            for ((robot, snapshot) in snapshotChannel) {
                val text = Json.encodeToString(RobotSnapshot.serializer(), snapshot)
                val file = getDataFile("snapshots-${robot.name}.json")
                file.appendText(text + "\n")
            }
        }

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
            trace("events") {
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
                                trace("scan") {
                                    val damage = tookDamage.remove(e.name)?.damage ?: 0.0
                                    robotService.onScan(e.name, e.toRobotScan(damage), battleField)
                                }
                            }
                        }
                    }
                }
            }

            trace("action") {
                coroutineScope {
                    radarStrategy.setMove()

                    if (movementEnabled) {
                        launch(Computation) {
                            trace("movement") {
                                movement.move()
                            }
                        }
                    }

                    if (targetingEnabled) {
                        launch(Computation) {
                            trace("targeting") {
                                val target = robotService.closest()
                                if (target != null) {
                                    val power = minOf(3.0, energy)
                                    val prediction = trace("aiming") { target.guns.best.predict(power) }
                                    val bullet = fire(prediction, power)
                                    if (bullet != null) trace("bullets") { target.guns.fire(power) }
                                    trace("waves") {
                                        target.waves.fire(power) {
                                            context[RealBullet] = bullet != null
                                        }
                                    }
                                }
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
        printVirtualGuns()
        printTimings()
    }

    private fun printTimings() {
        println(table {
            style {
                borderStyle = BorderStyle.Hidden
            }
            cellStyle {
                paddingLeft = 1
                paddingRight = 1
            }
            header {
                cellStyle { borderBottom = true }
                row {
                    cell("Name") { borderRight = true }
                    cell("Average")
                }
            }
            for ((name, avg) in Timer.timings) {
                row {
                    cell(name) { borderRight = true }
                    cell(avg)
                }
            }
        })
    }

    private fun printVirtualGuns() {
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
                        cell(gun.name) {
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
        val self = robotService.self
        val target = robotService.closest()

        if (target != null) {
//            drawPath(g, self, target)

//            g.draw(target.snapshot.wallProbe)

            val circle = escapeCircle(self.latest.location, target.latest.location, Rules.getBulletSpeed(3.0))
            val tangents =
                self.battleField.escape(self.latest.location, target.latest.location, Rules.getBulletSpeed(3.0))

            g.color = Color.blue
            g.draw(circle)
            for (p in tangents) {
                g.drawLine(self.latest.location, p)
            }

            g.color = Color.red
            g.draw(self.battleField.movable)
            for (p in tangents) {
                g.drawLine(target.latest.location, p)
            }
        }

        for (robot in robotService.alive) {
            val virtualGuns = robot.guns
            for ((index, gun) in virtualGuns.guns.reversed().withIndex()) {
                g.drawBullets(gun, time)
                g.drawSuccess(index, gun)
            }

            val virtualWaves = robot.waves
            for (wave in virtualWaves.waves) {
                g.drawWave(self.latest, wave, time)
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

val BulletHitEvent.damage: Double get() = Rules.getBulletDamage(bullet.power)

fun Wave.guessFactor(end: RobotScan): Double {
    val waveHeading = origin.theta(snapshot.scan.location)
    val bearing = Utils.normalRelativeAngle(origin.theta(end.location) - waveHeading)
    val escapeAngle = if (bearing < 0) escapeAngle.leftAngle else escapeAngle.rightAngle
    return (snapshot.gfDirection * bearing / escapeAngle).coerceIn(-1.0, 1.0)
}

private fun trainDimensionScales(
    network: NeuralNetwork,
    wave: Wave,
    neighbors: Collection<KdTree.Neighbor<RobotSnapshot>>,
    time: Long
) {
    val distance = wave.radius(time)
    val snapshot = wave.snapshot
    val delta = robotAngle(distance) / asin(TANK_MAX_SPEED / wave.speed)
    val gf = snapshot.guessFactor

    for (neighbor in neighbors) {
        val input = neighbor.value.guessFactorDimensions
        for (i in input.indices) {
            network.input[i] = sqr(input[i] - snapshot.guessFactorDimensions[i])
        }
        network.forward()
        if (abs(gf - neighbor.value.guessFactor) < delta) {
            network.error[0] = 0.0 - network.output[0] // good guess, dist -> 0.0
        } else {
            network.error[0] = 1.0 - network.output[0] // bad guess, dist -> 1.0
        }
        network.train(0.1)
    }

//    val weights = network.layers[0].weights
//    println(weights.zip(RobotSnapshot.DIMENSIONS) { w, d -> "${d.name}:${w.roundDecimals(5)}"})
}

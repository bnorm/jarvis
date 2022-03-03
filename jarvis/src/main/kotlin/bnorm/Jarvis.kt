package bnorm

import bnorm.coroutines.MyCoroutineRobot
import bnorm.coroutines.RobotTurn
import bnorm.debug.GuessFactorSnapshot
import bnorm.geo.Angle
import bnorm.geo.normalizeRelative
import bnorm.geo.times
import bnorm.kdtree.KdTree
import bnorm.neural.NeuralNetwork
import bnorm.parts.BattleField
import bnorm.parts.gun.CircularPrediction
import bnorm.parts.gun.DirectPrediction
import bnorm.parts.gun.GuessFactorPrediction
import bnorm.parts.gun.LinearPrediction
import bnorm.parts.gun.robotAngle
import bnorm.parts.gun.virtual.AttackWaves
import bnorm.parts.gun.virtual.VirtualGuns
import bnorm.parts.gun.virtual.VirtualWaves
import bnorm.parts.gun.virtual.Wave
import bnorm.parts.gun.virtual.attackWaves
import bnorm.parts.gun.virtual.virtualGuns
import bnorm.parts.gun.virtual.radius
import bnorm.parts.gun.virtual.waves
import bnorm.parts.radar.AdaptiveScan
import bnorm.parts.radar.Radar
import bnorm.parts.tank.MinimumRiskMovement
import bnorm.parts.tank.Movement
import bnorm.parts.tank.SurfableWave
import bnorm.parts.tank.TANK_MAX_SPEED
import bnorm.parts.tank.WaveSurfMovement
import bnorm.parts.tank.escape.EscapeEnvelope
import bnorm.parts.tank.escape.escape
import bnorm.parts.tank.escape.escapeAngle
import bnorm.plugin.Context
import bnorm.plugin.get
import bnorm.plugin.install
import bnorm.robot.AttackSnapshots
import bnorm.robot.Robot
import bnorm.robot.RobotScan
import bnorm.robot.RobotService
import bnorm.robot.RobotSnapshot
import bnorm.robot.RobotSnapshots
import bnorm.robot.attackSnapshot
import bnorm.robot.closest
import bnorm.robot.memorized
import bnorm.robot.robotSnapshot
import bnorm.robot.snapshot
import bnorm.robot.snapshot.BulletSnapshot
import bnorm.robot.snapshot.toSnapshot
import com.jakewharton.picnic.BorderStyle
import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import robocode.AdvancedRobot
import robocode.Bullet
import robocode.BulletHitBulletEvent
import robocode.BulletHitEvent
import robocode.HitByBulletEvent
import robocode.RobotDeathEvent
import robocode.RoundEndedEvent
import robocode.Rules
import robocode.ScannedRobotEvent
import robocode.SkippedTurnEvent
import robocode.StatusEvent
import java.awt.Color
import java.awt.Graphics2D
import java.io.BufferedWriter
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

val RealBullet = Context.Key<Boolean>("RealBullet")
val VirtualCluster = Context.Key<Collection<KdTree.Neighbor<RobotSnapshot>>>("VirtualCluster")
val RealCluster = Context.Key<Collection<KdTree.Neighbor<RobotSnapshot>>>("RealCluster")
val BulletCluster = Context.Key<Collection<KdTree.Neighbor<RobotSnapshot>>>("BulletCluster")
val WaveHeading = Context.Key<Angle>("WaveHeading")
val WaveSnapshot = Context.Key<RobotSnapshot>("WaveSnapshot")

// Movement only version of Jarvis
class JarvisM : Jarvis(targetingEnabled = false)

// Targeting only version of Jarvis
class JarvisT : Jarvis(movementEnabled = false)

open class Jarvis @JvmOverloads constructor(
    private val targetingEnabled: Boolean = true,
    private val movementEnabled: Boolean = true,
) : MyCoroutineRobot() {
    companion object {
        private val robotService = RobotService(
            onSelf = { robot ->
            },
            onEnemy = { robot ->
                val service = this

                robot.install(AttackSnapshots) {
                    self = service.self

                    factory = AttackSnapshots.Factory { scan, prevSnapshot ->
                        robotSnapshot(robot, scan, prevSnapshot)
                    }
                }

                robot.install(AttackWaves) {
                    self = service.self

                    val virtualNetwork = NeuralNetwork(RobotSnapshot.DIMENSIONS.size, 1) { _, _ -> 1.0 }
                    val virtualTree = KdTree(
                        virtualNetwork.layers[0].weights,
                        KdTree.DEFAULT_BUCKET_SIZE,
                        RobotSnapshot::guessFactorDimensions
                    )

                    val realNetwork = NeuralNetwork(RobotSnapshot.DIMENSIONS.size, 1) { _, _ -> 1.0 }
                    val realTree = KdTree(
                        realNetwork.layers[0].weights,
                        KdTree.DEFAULT_BUCKET_SIZE,
                        RobotSnapshot::guessFactorDimensions
                    )

                    val bulletNetwork = NeuralNetwork(RobotSnapshot.DIMENSIONS.size, 1) { _, _ -> 1.0 }
                    val bulletTree = KdTree(
                        bulletNetwork.layers[0].weights,
                        KdTree.DEFAULT_BUCKET_SIZE,
                        RobotSnapshot::guessFactorDimensions
                    )

                    val virtualCluster by robot.memorized {
                        trace("self-knn-virt") {
                            val prev = robot.attackSnapshot.prev
                            if (prev != null) virtualTree.neighbors(prev, 50)
                            else emptyList()
                        }
                    }

                    val realCluster by robot.memorized {
                        trace("self-knn-real") {
                            val prev = robot.attackSnapshot.prev
                            if (prev != null) realTree.neighbors(prev, 50)
                            else emptyList()
                        }
                    }

                    val bulletCluster by robot.memorized {
                        trace("self-knn-bullet") {
                            val prev = robot.attackSnapshot.prev
                            if (prev != null) bulletTree.neighbors(prev, 50)
                            else emptyList()
                        }
                    }

                    onWave = { real ->
                        val snapshot = robot.attackSnapshot.let { if (real) it.prev!! else it }
                        val scan = robot.latest.let { if (real) it.prev!! else it }

                        context[RealBullet] = real
                        context[WaveSnapshot] = snapshot
                        context[WaveHeading] = origin.theta(snapshot.scan.location)
                        context[VirtualCluster] = virtualCluster
//                        if (real) {
                        context[RealCluster] = realCluster
                        context[BulletCluster] = bulletCluster
//                        }
                        context[EscapeEnvelope.key] = self.battleField.escape(
                            source = scan.location,
                            target = snapshot.scan.location,
                            speed = speed
                        )
                    }

                    listen { wave ->
                        val snapshot = wave[WaveSnapshot]
                        if (snapshot.guessFactor.isNaN()) {
                            snapshot.guessFactor = wave.guessFactor(self.latest.location)
                        }

                        if (wave.context[RealBullet]) {
                            realTree.add(snapshot)
                        }
                        virtualTree.add(snapshot)
                    }

                    listen { wave, bullet ->
                        val snapshot = wave[WaveSnapshot]
                        snapshot.guessFactor = wave.guessFactor(bullet.location)
                        bulletTree.add(snapshot)
                    }
                }

                robot.install(RobotSnapshots) {
                    factory = RobotSnapshots.Factory { scan, prevSnapshot ->
                        robotSnapshot(self, scan, prevSnapshot)
                    }
                }

                pounce(robot)
            })

        //        val snapshotChannel = Channel<Pair<Robot, RobotSnapshot>>(Channel.UNLIMITED)
        val movementOne = mutableMapOf<String, WaveSurfMovement>()
    }

    override suspend fun init(): RobotTurn {
//        GlobalScope.launch(Computation) { exportSnapshots(snapshotChannel) }

        val battleField = BattleField()
        val movementMelee = MinimumRiskMovement(battleField, robotService.alive)

        val radar = Radar(this)
        val radarStrategy = AdaptiveScan(radar, robotService.alive) {
            val robots = robotService.alive
            when {
                robots.size == 1 -> robots.first()
                // Gun is within 4 ticks of firing, target closest robot
//                gunHeat - gunCoolingRate * 4 <= 0 -> robotService.closest()
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
                    val bulletHits = mutableMapOf<String, BulletSnapshot>()
                    val hitByBullets = mutableMapOf<String, BulletSnapshot>()
                    events.collect { e ->
                        when (e) {
                            is RoundEndedEvent -> {
                                robotService.onRoundEnd()

                                printVirtualGuns()
                                printTimings()
                            }
                            is StatusEvent -> {
                                robotService.onStatus(name, e.toRobotScan(), battleField)
                            }
                            is RobotDeathEvent -> robotService.onKill(e.name)
                            is BulletHitBulletEvent -> {
                            }
                            is BulletHitEvent -> bulletHits[e.bullet.victim] = e.bullet.toSnapshot()
                            is HitByBulletEvent -> hitByBullets[e.bullet.name] = e.bullet.toSnapshot()
                            is ScannedRobotEvent -> launch(Computation) {
                                trace("events.scan") {
                                    val hitByBullet = hitByBullets.remove(e.name)
                                    val bulletHit = bulletHits.remove(e.name)
                                    robotService.onScan(
                                        e.name,
                                        e.toRobotScan(this@Jarvis, hitByBullet, bulletHit),
                                        battleField
                                    )
                                }
                            }
                        }
                    }
                }
            }

            trace("parts") {
                coroutineScope {
                    launch(Computation) {
                        trace("parts.radar") {
                            radarStrategy.setMove()
                        }
                    }

                    val target = robotService.closest()
                    if (target != null) {
                        if (movementEnabled) {
                            launch(Computation) {
                                trace("parts.tank") {
                                    val movement = when (robotService.alive.size) {
                                        1 -> movementOne.getOrPut(target.name) {
                                            WaveSurfMovement(robotService.self, target) {
                                                val waves = target.attackWaves.waves.filter { it.context[RealBullet] }
                                                waves.map { SurfableWave(it, it.context[BulletCluster]) }
                                            }
                                        }
                                        else -> movementMelee
                                    }

                                    movement.move()
                                }
                            }
                        }

                        if (targetingEnabled) {
                            launch(Computation) {
                                trace("parts.gun") {
                                    val power = pickPower(target)
                                    val prediction = trace("aiming") { target.virtualGuns.best.predict(power) }
                                    val bullet = fire(prediction, power)
                                    if (bullet != null) trace("bullets") { target.virtualGuns.fire(power) }
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

    private fun pickPower(target: Robot): Double {
        if (!movementEnabled) return minOf(3.0, energy)

        val power = inversePower(target.latest.energy)
        return power.coerceAtMost(minOf(1.8, energy))
    }

    private fun inversePower(damage: Double): Double =
        if (damage > 4.0) (damage - 2.0) / 6.0 else damage / 4.0

    private suspend fun fire(predicted: Vector, power: Double): Bullet? {
        return withContext(Main) {
            val bullet = if (gunTurnRemainingRadians == 0.0) setFireBullet(power) else null
            setTurnGunRightRadians((predicted.theta - Angle(gunHeadingRadians)).normalizeRelative().radians)
            bullet
        }
    }

    suspend fun Movement.move() {
        val latest = robotService.self.latest
        val move = invoke(latest.location, latest.velocity)
        withContext(Main) {
            setTurnRightRadians(move.theta.radians)
            setAhead(move.r)
        }
    }

    override fun onSkippedTurn(event: SkippedTurnEvent) {
        println("SKIPPED! ${event.skippedTurn} ${time}")
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
                val virtualGuns = robot.virtualGuns
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

    private suspend fun exportSnapshots(snapshots: Channel<Pair<Robot, RobotSnapshot>>) {
        val writersByName = mutableMapOf<String, BufferedWriter>().withDefault { key ->
            val file = getDataFile("snapshots-$key.json")
            println("data file: ${file.absoluteFile}")
            file.outputStream().bufferedWriter().also { writer ->
                val text = Json.encodeToString(
                    ListSerializer(String.serializer()),
                    RobotSnapshot.DIMENSIONS.map { it.name })
                writer.appendLine(text)
            }
        }

        try {
            // TODO split each robot out into separate coroutine
            for ((robot, snapshot) in snapshots) {
                val writer = writersByName.getValue(robot.name)
                val text = Json.encodeToString(
                    GuessFactorSnapshot.serializer(),
                    GuessFactorSnapshot(snapshot.guessFactorDimensions, snapshot.guessFactor)
                )
                writer.appendLine(text)
            }
        } finally {
            for (writer in writersByName.values) {
                runCatching { writer.close() }
            }
        }
    }

    override fun onPaint(g: Graphics2D) {
        val time = time
        val self = robotService.self
        val target = robotService.closest()

        g.color = Color.red
        g.draw(self.battleField.movable)

        if (target != null) {
            val movement = movementOne[target.name]
            if (movement != null) {
                g.color = Color.red
                g.draw(self, movement.backward)
                g.color = Color.green
                g.draw(self, movement.forward)
                g.color = Color.yellow
                g.draw(self, movement.stop)
            }

//            g.draw(target.snapshot.wallProbe)
//            g.drawPath(target, self, target.attackSnapshot.moveDirection)
            g.color = Color.white
            val source = target.latest.location

            g.drawCircle(source, 500.0)

            val attackWaves = target.attackWaves
//            val wave = attackWaves.predicted
            for (wave in attackWaves.waves) {
                g.drawWave(self.latest, wave, time)

//                g.color = Color.blue
//                g.drawCircle(wave.origin, wave.radius(time))
//
//                val heading = theta(wave.origin, self.latest.location)
//                val envelope = self.battleField.escape(
//                    wave.origin.project(heading, wave.radius(time)),
//                    self.latest.location,
//                    wave.speed
//                )
//
//                g.draw(
//                    envelope,
//                    wave.origin,
//                    wave.snapshot.scan.location,
//                    wave.context.find(RealCluster)?.buckets(31),
//                    wave.snapshot.gfDirection,
//                )
            }
        }

//        for (robot in robotService.alive) {
//            val virtualGuns = robot.guns
//            for ((index, gun) in virtualGuns.guns.reversed().withIndex()) {
//                g.drawBullets(gun, time)
//                g.drawSuccess(index, gun)
//            }
//
//            val virtualWaves = robot.waves
//            for (wave in virtualWaves.waves) {
//                g.drawWave(self.latest, wave, time)
//            }
//        }

//        val target = robotService.closest(x, y)
//        if (target != null) {
//            val snapshot = target.latest.toGuessFactorPoint(0.0)
//            val neighbors = tree.neighbors(snapshot).take(100).toList()
////            println("snapshot=$snapshot neighbors=${neighbors.joinToString { "(${it.distSqr},${it.value})" }}")
//            g.drawCluster(target.latest, neighbors)
//        }
    }
}

val BulletHitEvent.damage: Double get() = Rules.getBulletDamage(bullet.power)
val BulletHitEvent.energy: Double get() = 3 * bullet.power
val HitByBulletEvent.damage: Double get() = Rules.getBulletDamage(bullet.power)
val HitByBulletEvent.energy: Double get() = 3 * bullet.power

fun Wave.guessFactor(location: Vector.Cartesian, waveHeading: Angle = context[WaveHeading]): Double {
    val bearing = (origin.theta(location) - waveHeading).normalizeRelative()
    val escapeAngle = if (bearing < Angle.ZERO) escapeAngle.leftAngle else escapeAngle.rightAngle
    val snapshot = this[WaveSnapshot]
    return (snapshot.gfDirection.toDouble() * bearing / escapeAngle).coerceIn(-1.0, 1.0)
}

fun Wave.guessFactor(bearing: Angle): Double {
    val escapeAngle = if (bearing < Angle.ZERO) escapeAngle.leftAngle else escapeAngle.rightAngle
    val snapshot = this[WaveSnapshot]
    return (snapshot.gfDirection.toDouble() * bearing / escapeAngle).coerceIn(-1.0, 1.0)
}

fun AdvancedRobot.toRobotScan(): RobotScan {
    return RobotScan(
        location = Cartesian(x, y),
        velocity = Polar(Angle(headingRadians), velocity),
        energy = energy,
        time = time,
        interpolated = false,
    )
}

fun StatusEvent.toRobotScan(): RobotScan {
    return RobotScan(
        location = Cartesian(status.x, status.y),
        velocity = Polar(Angle(status.headingRadians), status.velocity),
        energy = status.energy,
        time = status.time,
        interpolated = false,
    )
}

fun ScannedRobotEvent.toRobotScan(
    robot: AdvancedRobot,
    bulletHit: BulletSnapshot? = null,
    hitByBullet: BulletSnapshot? = null,
): RobotScan {
    val angle = robot.headingRadians + bearingRadians
    return RobotScan(
        location = Cartesian(robot.x + sin(angle) * distance, robot.y + cos(angle) * distance),
        velocity = Polar(Angle(headingRadians), velocity),
        energy = energy,
        time = time,
        interpolated = false,
        bulletHit = bulletHit,
        hitByBullet = hitByBullet,
    )
}

private fun trainDimensionScales(
    network: NeuralNetwork,
    wave: Wave,
    neighbors: Collection<KdTree.Neighbor<RobotSnapshot>>,
    time: Long,
) {
    val distance = wave.radius(time)
    val snapshot = wave[WaveSnapshot]
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


private suspend fun RobotService.pounce(robot: Robot) {
    val service = this
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

    val virtualCluster by robot.memorized {
        trace("neighbors-virtual") { virtualTree.neighbors(robot.snapshot, 50) }
    }
    val realCluster by robot.memorized {
        trace("neighbors-real") { realTree.neighbors(robot.snapshot, 50) }
    }
    val cluster by robot.memorized {
        trace("neighbors") { realCluster + virtualCluster }
    }

    robot.install(VirtualGuns) {
        self = service.self
        predictions = mapOf(
            "Direct" to DirectPrediction(service.self, robot),
            "Hybrid GF" to GuessFactorPrediction(service.self, robot) { cluster },
            "Virtual GF" to GuessFactorPrediction(service.self, robot) { realCluster },
            "Real GF" to GuessFactorPrediction(service.self, robot) { virtualCluster },
            "Circular" to CircularPrediction(service.self, robot),
            "Linear" to LinearPrediction(service.self, robot),
        )
    }

    robot.install(VirtualWaves) {
        self = service.self
        onWave = {
            val snapshot = robot.snapshot
            context[WaveSnapshot] = snapshot
            context[WaveHeading] = origin.theta(snapshot.scan.location)
            context[VirtualCluster] = virtualCluster
            if (context[RealBullet]) {
                context[RealCluster] = realCluster
            }
            context[EscapeEnvelope.key] =
                service.self.battleField.escape(service.self.latest.location, robot.latest.location, speed)
        }
        listen { wave ->
            val snapshot = wave[WaveSnapshot]
            snapshot.guessFactor = wave.guessFactor(robot.latest.location)

            if (wave.context[RealBullet]) {
                realTree.add(snapshot)
            }
            virtualTree.add(snapshot)

            // TODO save snapshot to file
//            Jarvis.snapshotChannel.send(robot to snapshot)
        }
    }
}

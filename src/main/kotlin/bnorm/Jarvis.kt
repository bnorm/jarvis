package bnorm

import bnorm.kdtree.KdTree
import bnorm.neural.Activation
import bnorm.neural.NeuralNetwork
import bnorm.parts.BattleField
import bnorm.parts.gun.GuessFactorPrediction
import bnorm.parts.gun.Gun
import bnorm.parts.gun.escapeAngle
import bnorm.parts.gun.robotAngle
import bnorm.parts.gun.virtual.RobotWaveManager
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

data class WaveData(
    val scan: RobotScan,
    val snapshot: RobotSnapshot,
    val cluster: List<KdTree.Neighbor<RobotSnapshot>>,
)

class Jarvis : AdvancedRobot() {
    companion object {
        private val robotService = RobotService { robot ->
            robot.install(RobotSnapshots) {
                factory = RobotSnapshots.Factory { currScan, prevScan, prevSnapshot ->
                    robotSnapshot(currScan, prevSnapshot, prevScan)
                }
            }
        }

        private val network =
            NeuralNetwork(RobotSnapshot.DIMENSIONS.size, 1, activation = Activation.Sigmoid) { _, _ -> 1.0 }

        private val tree = KdTree(RobotSnapshot.DIMENSIONS.size, RobotSnapshot::dimensions) { p1, p2 ->
            dist(p1, p2)
        }

        private val dimensionScale = DoubleArray(RobotSnapshot.DIMENSIONS.size) { 1.0 }
        private fun dist(p1: DoubleArray, p2: DoubleArray): Double {
            var sum = 0.0
            for (i in dimensionScale.indices) {
                sum += dimensionScale[i] * sqr(p1[i] - p2[i])
            }
            return sum
        }
    }

    private var battleField: BattleField? = null

    private val gun = Gun(this)
    private val virtualGunService = GuessFactorPrediction(gun, tree) { it.context[RobotSnapshots].latest }
//        VirtualGunService(
//        gun, robotService, listOf(
////            DirectPrediction(gun),
////            LinearPrediction(gun),
////            CircularPrediction(gun),
//            GuessFactorPrediction(gun, tree) { it.context[RobotSnapshots].latest }
//        )
//    )

    private val waveServices = mutableMapOf<String, RobotWaveManager<WaveData>>()

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
//            movementStrategy.setMove()
            radarStrategy.setMove()

            val target = robotService.closest(x, y)
            if (target != null) {
                val bulletPower = 3.0

                val predicted = virtualGunService.predict(target, 3.0)
                setTurnGunRightRadians(Utils.normalRelativeAngle(predicted.theta - gunHeadingRadians))

                if (abs(gunTurnRemainingRadians) < PI / 180) {
                    val bullet = setFireBullet(bulletPower)
                    if (bullet != null) {
                        val snapshot = target.context[RobotSnapshots].latest
                        val predictions = tree.neighbors(snapshot).take(100).toList()

                        waveServices.getOrPut(target.name) {
                            RobotWaveManager<WaveData>(robotService.self, target).apply {
                                listen { wave ->
                                    val waveSnapshot = wave.value.snapshot
                                    waveSnapshot.guessFactor = wave.guessFactor(target.latest)
                                    tree.add(waveSnapshot)
                                    trainDimensionScales(wave)
                                    tree.print()
                                }
                            }
                        }.fire(bulletPower, WaveData(target.latest, snapshot, predictions))

//                    virtualGunService.fire(bulletPower)
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
//        for ((name, holders) in virtualGunService.robots) {
//            for (holder in holders) {
//                when (holder.strategy) {
//                    is DirectPrediction -> g.color = Color.red
//                    is LinearPrediction -> g.color = Color.orange
//                    is CircularPrediction -> g.color = Color.yellow
//                    is GuessFactorPrediction<*> -> g.color = Color.green
//                    else -> g.color = Color.white
//                }
//
//                for (bullet in holder.virtualGun.bullets) {
//                    g.drawLine(bullet.location(time), bullet.location(time + 1))
//                }
//            }
//        }

        val time = time
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
//        virtualGunService.scan(e.name, scan)
    }

    override fun onRobotDeath(e: RobotDeathEvent) {
        robotService.onKill(e.name)
//        virtualGunService.death(e.name)
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

    private fun NeuralNetwork.train(
        wave: Wave<WaveData>
    ) {
        val distance = wave.radius(time)
        val snapshot = wave.value.snapshot

        val delta = robotAngle(distance) / escapeAngle(TANK_MAX_SPEED)

        val gf = snapshot.guessFactor
        val point = snapshot.dimensions
        for (neighbor in wave.value.cluster) {
            // run network forward
            dist(point, neighbor.value.dimensions)
//            print("d$dist")
            if (abs(gf - neighbor.value.guessFactor) < delta) {
//                print("G ")
                output[0] = -0.1 // nudge that this was a good result
            } else {
//                print("B ")
                output[0] = 0.1 // nudge that this was a bad result
            }
            network.train(0.1)

//            for (layer in network.layers) {
//                for (weight in layer.weights) {
//                    for (i in weight.indices) {
//                        if (weight[i] < 0.0) weight[i] = 0.0
//                    }
//                }
//            }
        }
//        println()

        for ((l, layer) in layers.withIndex()) {
            println("layer ${l + 1}")
            for (weight in layer.weights) {
                println(weight.toList())
            }
        }
    }

    private fun trainDimensionScales(
        wave: Wave<WaveData>
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

        for ((value, dist) in wave.value.cluster) {
            val dimensions = value.dimensions
            updateVariances(++t, tVariances, tMeans, dimensions)

            if (abs(gf - value.guessFactor) < delta) {
                updateVariances(++g, gVariances, gMeans, dimensions)
            } else {
                updateVariances(++b, bVariances, bMeans, dimensions)
            }
        }

        // if 5% of predictions where good...
        if (g >= t / 20) {
            for (i in dimensionScale.indices) {
                if (gVariances[i] > 0.0) {
                    val scale = dimensionScale[i]
                    val weight = 1.0 / (1.0 + abs(1.0 - scale))

                    val gDist = sqr(gMeans[i] - snapshot.dimensions[i])
                    val bDist = sqr(bMeans[i] - snapshot.dimensions[i])

                    // ...and good predictions had less variance and were closer to the snapshot
                    if (gVariances[i] < bVariances[i] && gDist < bDist && gDist < gVariances[i]) {
                        dimensionScale[i] = (scale + weight * 0.05).coerceAtMost(2.0)
                    } else if (gVariances[i] > bVariances[i] && gDist > bDist && gDist > gVariances[i]) {
                        dimensionScale[i] = (scale - weight * 0.05).coerceAtLeast(0.1)
                    }
                }
            }

            val sum = dimensionScale.sum()
            for (i in dimensionScale.indices) {
                dimensionScale[i] = (dimensionScale[i] * dimensionScale.size / sum).coerceAtLeast(0.1)
            }
        }

        println(table {
            cellStyle {
                border = true
            }
            row { cell("Dimension") { columnSpan = 3 }; RobotSnapshot.DIMENSIONS.forEach { cell(it.name) } }
            row { cell("Scale") { columnSpan = 3 }; dimension(dimensionScale) }
            row { cell("Target") { columnSpan = 3 }; dimension(snapshot.dimensions) }
            row { cell("Mean") { rowSpan = 3 }; cell("Good"); cell(g); dimension(gMeans) }
            row { cell("Bad"); cell(b); dimension(bMeans) }
            row { cell("All"); cell(t); dimension(tMeans) }
            row { cell("Variance") { rowSpan = 3 }; cell("Good"); cell(g); dimension(gVariances) }
            row { cell("Bad"); cell(b); dimension(bVariances) }
            row { cell("All"); cell(t); dimension(tVariances) }
        })
        println()
    }
}

private fun RowDsl.dimension(values: DoubleArray) {
    for (v in values) {
        cell((v * 1000).toInt() / 1000.0)
    }
}

private fun <T : Comparable<T>> Iterable<T>.increasing(): Boolean {
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

private fun <T, C : Comparable<C>> Iterable<T>.increasingBy(selector: (T) -> C): Boolean {
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

fun Wave<WaveData>.guessFactor(end: RobotScan): Double {
    val waveHeading = origin.theta(value.scan.location)

    val waveBearing = Utils.normalRelativeAngle(origin.theta(end.location) - waveHeading)
    val escapeAngle = escapeAngle(TANK_MAX_SPEED)

    val direction = value.snapshot.rotateDirection
    return (direction * waveBearing / escapeAngle).coerceIn(-1.0, 1.0)
}

fun updateVariances(
    n: Int,
    variances: DoubleArray,
    means: DoubleArray,
    values: DoubleArray
) {
    if (n == 1) {
        for (i in variances.indices) {
            val v = values[i]
            val oldMean = means[i]
            means[i] = (v + (n - 1) * oldMean) / n
        }
    } else {
        for (i in variances.indices) {
            val v = values[i]
            val oldMean = means[i]
            means[i] = (v + (n - 1) * oldMean) / n
            variances[i] = (n - 2) * variances[i] / (n - 1) + sqr(v - oldMean) / n
        }
    }
}

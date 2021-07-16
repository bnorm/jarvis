package bnorm.parts.tank

import bnorm.Vector
import bnorm.WaveHeading
import bnorm.guessFactor
import bnorm.kdtree.KdTree
import bnorm.parts.gun.GuessFactorSnapshot
import bnorm.parts.gun.gauss
import bnorm.parts.gun.virtual.Wave
import bnorm.parts.gun.virtual.WaveContext
import bnorm.parts.gun.virtual.radius
import bnorm.parts.tank.escape.EscapeEnvelope
import bnorm.parts.tank.escape.escape
import bnorm.r
import bnorm.r2
import bnorm.robot.Robot
import bnorm.robot.RobotSnapshots
import bnorm.robot.attackSnapshot
import bnorm.sqr
import bnorm.theta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.transformWhile
import robocode.Rules
import java.util.*
import kotlin.math.abs
import kotlin.math.asin

data class SurfableWave(
    val wave: Wave,
    val cluster: Collection<KdTree.Neighbor<GuessFactorSnapshot>>,
)

class WaveSurfMovement(
    private val self: Robot,
    private val target: Robot,
    private val distance: Double = 500.0,
    private val wavesFunction: () -> List<SurfableWave>,
) : Movement {
    val stop = WallSmoothMovement(target.battleField, OrbitMovement(target, distance, 0.0))
    val forward = WallSmoothMovement(target.battleField, OrbitMovement(target, distance, 1.0))
    val backward = WallSmoothMovement(target.battleField, OrbitMovement(target, distance, -1.0))

    override suspend fun invoke(location: Vector.Cartesian, velocity: Vector.Polar): Vector.Polar {
        val waves = wavesFunction().takeIf { it.isNotEmpty() } ?: listOf(defaultWave())
//            .filter { sqr(it.wave.radius(self.latest.time + 3)) <= it.wave.origin.r2(self.latest.location) }

        val choice = coroutineScope {
            minimum(
                Option(backward, risk(backward, self.latest.time, waves)),
                Option(forward, risk(forward, self.latest.time, waves)),
                Option(stop, risk(stop, self.latest.time, waves)),
            )
        }

        return (choice ?: stop).invoke(location, velocity)
    }

    private fun defaultWave(): SurfableWave {
        val sourceLocation = target.latest.location
        val targetLocation = self.latest.location
        val context = WaveContext()
        val wave = Wave(
            origin = sourceLocation,
            speed = Rules.getBulletSpeed(3.0),
            time = self.latest.time,
            context = context
        )

        context[RobotSnapshots] = target.attackSnapshot
        context[WaveHeading] = targetLocation.theta(sourceLocation)
        context[EscapeEnvelope] = self.battleField.escape(
            source = sourceLocation,
            target = targetLocation,
            speed = wave.speed
        )

        return SurfableWave(wave, emptyList())
    }

    private suspend fun Option(
        key: Movement,
        remaining: ReceiveChannel<Double>,
    ): Option<Movement, Double>? {
        val next = remaining.receiveOrNull() ?: return null
        return Option(key, next, remaining)
    }

    private class Option<K, V : Comparable<V>>(
        val key: K,
        var head: V,
        val remaining: ReceiveChannel<V>,
    ) : Comparable<Option<K, V>> {
        override fun compareTo(other: Option<K, V>): Int =
            compareValues(head, other.head)
    }

    private suspend fun <K : Any, V : Comparable<V>> minimum(
        vararg options: Option<K, V>?,
    ): K? {
        val queue = PriorityQueue<Option<K, V>>(Comparator.reverseOrder())
        options.forEach { if (it != null) queue.add(it) }

        while (queue.size > 1) {
            val head = queue.remove()
            val next = head.remaining.receiveOrNull() ?: continue
            head.head = next
            queue.add(head)
        }

        return queue.poll()?.key
    }

    private fun CoroutineScope.risk(
        movement: WallSmoothMovement,
        startTime: Long,
        waves: List<SurfableWave>,
    ): ReceiveChannel<Double> {
        var currTime = startTime
        return self.simulate(movement)
            .transformWhile { location ->
                if (waves.waveBreak(location, currTime++)) {
                    val danger = waves.sumOf { it.toDanger(location, startTime) }
                    emit(danger)
                    true
                } else false
            }
            .produceIn(this)
    }

    private fun List<SurfableWave>.waveBreak(location: Vector.Cartesian, time: Long): Boolean =
        isNotEmpty() && all { sqr(it.wave.radius(time)) <= it.wave.origin.r2(location) }

    private fun SurfableWave.toDanger(location: Vector.Cartesian, time: Long): Double {
        val radius = wave.origin.r(location)
        // val boost = 1.0 / sqr(gauss(distance, distance, 200.0, radius))

        val gf = wave.guessFactor(location)
        val gfWidth = abs(wave.guessFactor(asin(TANK_SIZE / 2 / radius)))
//        println("w=$gfWidth")

        var sum = 0.0
        for (point in cluster) {
            val peek = 1.0 / point.dist
            sum += gauss(peek, 0.01, (abs(point.value.guessFactor - gf) - gfWidth).coerceAtLeast(0.0))
//                .coerceAtLeast(peek * 1e-16)
        }
        if (cluster.isEmpty()) {
            sum += 1.0 / sqr(gauss(distance, distance, 200.0, radius))
//            sum += gauss(1.0, 0.01, 2.0 - gfWidth)
//                .coerceAtLeast(0.001)
        }
        return sum / sqr(radius - wave.radius(time))
    }
}

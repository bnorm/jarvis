package bnorm.parts.tank

import bnorm.Vector
import bnorm.WaveHeading
import bnorm.WaveSnapshot
import bnorm.geo.asin
import bnorm.guessFactor
import bnorm.kdtree.KdTree
import bnorm.parts.gun.GuessFactorSnapshot
import bnorm.parts.gun.gauss
import bnorm.parts.gun.virtual.Wave
import bnorm.parts.gun.virtual.radius
import bnorm.parts.tank.escape.EscapeEnvelope
import bnorm.parts.tank.escape.escape
import bnorm.r
import bnorm.r2
import bnorm.robot.Robot
import bnorm.robot.attackSnapshot
import bnorm.sqr
import bnorm.theta
import robocode.Rules
import java.util.PriorityQueue
import kotlin.math.abs

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

    override fun invoke(location: Vector.Cartesian, velocity: Vector.Polar): Vector.Polar {
        val waves = wavesFunction().takeIf { it.isNotEmpty() } ?: listOf(defaultWave())
//            .filter { sqr(it.wave.radius(self.latest.time + 3)) <= it.wave.origin.r2(self.latest.location) }

        val choice = minimum(
            Option(backward, risk(backward, self.latest.time, waves)),
            Option(forward, risk(forward, self.latest.time, waves)),
            Option(stop, risk(stop, self.latest.time, waves)),
        )

        return (choice ?: stop).invoke(location, velocity)
    }

    private fun defaultWave(): SurfableWave {
        val sourceLocation = target.latest.location
        val targetLocation = self.latest.location
        val wave = Wave(
            origin = sourceLocation,
            speed = Rules.getBulletSpeed(3.0),
            time = self.latest.time,
        )

        val context = wave.context
        context[WaveSnapshot] = target.attackSnapshot
        context[WaveHeading] = targetLocation.theta(sourceLocation)
        context[EscapeEnvelope.key] = self.battleField.escape(
            source = sourceLocation,
            target = targetLocation,
            speed = wave.speed
        )

        return SurfableWave(wave, emptyList())
    }

    private fun Option(
        key: Movement,
        remaining: Iterator<Double>,
    ): Option<Movement, Double>? {
        val next = if (remaining.hasNext()) remaining.next() else return null
        return Option(key, next, remaining)
    }

    private class Option<K, V : Comparable<V>>(
        val key: K,
        var value: V,
        val remaining: Iterator<V>,
    ) : Comparable<Option<K, V>> {
        override fun compareTo(other: Option<K, V>): Int =
            compareValues(value, other.value)
    }

    private fun <K : Any, V : Comparable<V>> minimum(
        vararg options: Option<K, V>?,
    ): K? {
        val queue = PriorityQueue<Option<K, V>>(Comparator.reverseOrder())
        options.forEach { if (it != null) queue.add(it) }

        while (queue.size > 1) {
            val head = queue.remove()
            val next = if (head.remaining.hasNext()) head.remaining.next() else continue
            head.value = next
            queue.add(head)
        }

        return queue.poll()?.key
    }

    private fun risk(
        movement: WallSmoothMovement,
        startTime: Long,
        waves: List<SurfableWave>,
    ): Iterator<Double> {
        var currTime = startTime
        return self.simulate(movement)
            .takeWhile { location -> waves.waveBreak(location, currTime++) }
            .map { location -> waves.sumOf { it.toDanger(location, startTime) } }
            .iterator()
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

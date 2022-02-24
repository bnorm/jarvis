package bnorm.kdtree

import org.openjdk.jmh.Main
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@Fork(1)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class KdTreePerfTest {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = Main.main(arrayOf(KdTreePerfTest::class.java.name))
    }

    data class Point(val x: Double, val y: Double) {
        val dimensions: DoubleArray = doubleArrayOf(x, y)
    }

    private val pointScales = doubleArrayOf(1.0, 1.0)
    private val points = buildList {
        repeat(1000) { x ->
            repeat(1000) { y ->
                add(Point((x - 500.0) / 500.0, (y - 500.0) / 500.0))
            }
        }
    }
    private val tree = KdTree(pointScales, 100, Point::dimensions).apply {
        for (point in points.shuffled(Random(0))) {
            add(point)
        }
    }

    @Benchmark
    fun neighborsList() = tree.neighbors(Point(0.0, 0.0), 100)

    @Benchmark
    fun neighborsSequence() = tree.neighbors(Point(0.0, 0.0)).take(100).toList()
}

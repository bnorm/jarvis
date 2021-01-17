package bnorm

import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.time.nanoseconds

object Timer {
    data class Mark(
        val name: String,
        val time: TimeMark,
    )

    private data class Accumulation(
        val total: AtomicLong = AtomicLong(),
        val count: AtomicLong = AtomicLong(),
    ) {
        val average: Duration get() = (total.get() / count.get().toDouble()).nanoseconds
    }

    private val _timings = mutableMapOf<String, Accumulation>()
    val timings: Map<String, Duration> get() = _timings.mapValues { (k, v) -> v.average }

    fun start(name: String) = Mark(name, TimeSource.Monotonic.markNow())
    fun end(mark: Mark) {
        val (total, count) = _timings.getOrPut(mark.name) { Accumulation() }
        total.getAndAdd(mark.time.elapsedNow().toLongNanoseconds())
        count.getAndIncrement()
    }
}

inline fun <R> trace(name: String, block: () -> R): R {
    val mark = Timer.start(name)
    try {
        return block()
    } finally {
        Timer.end(mark)
    }
}

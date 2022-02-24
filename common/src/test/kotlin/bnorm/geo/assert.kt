package bnorm.geo

import bnorm.Vector
import bnorm.r2
import kotlin.test.assertEquals

private val comparator = compareBy<Vector.Cartesian> { it.x }.thenBy { it.y }

fun assertClose(
    expected: Set<Vector.Cartesian>,
    actual: Set<Vector.Cartesian>,
    delta: Double = 1e-12,
) {
    val sortedExpected = expected.sortedWith(comparator)
    val sortedActual = actual.sortedWith(comparator)

    val delta2 = delta * delta
    val distances = sortedActual.zip(sortedExpected) { a, e -> a.r2(e) }
    if (distances.any { it > delta2 }) {
        assertEquals(sortedExpected, sortedActual)
    }
}

fun assertClose(
    expected: Vector.Cartesian?,
    actual: Vector.Cartesian?,
    delta: Double = 1e-12,
) {
    val delta2 = delta * delta
    val distance = expected?.let { actual?.r2(it) } ?: Double.MAX_VALUE
    if (distance > delta2) {
        assertEquals(expected, actual)
    }
}

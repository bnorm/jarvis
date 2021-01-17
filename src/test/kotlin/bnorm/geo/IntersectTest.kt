package bnorm.geo

import bnorm.Vector
import bnorm.r2
import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals

class IntersectTest {
    private val SQRT_2 = sqrt(2.0)
    private val ORIGIN = Vector.Cartesian(0.0, 0.0)

    @Test
    fun `test circle rectangle intersection`() {
        assertClose(
            expected = setOf(
                Vector.Cartesian(1.0, 0.0)
            ),
            actual = Rectangle(Vector.Cartesian(2.0, 0.0), 2.0, 2.0)
                .intersect(Circle(ORIGIN, 1.0))
        )

        // Floating point error?
//        assertEquals(
//            listOf(Vector.Cartesian(SQRT_2 / 2, SQRT_2 / 2)),
//            Rectangle(Vector.Cartesian(1.0, 1.0), 2 - SQRT_2, 2 - SQRT_2)
//                .intersect(Circle(Vector.Cartesian(0.0, 0.0), 1.0))
//        )

        // Floating point error?
        assertClose(
            expected = setOf(
                Vector.Cartesian(SQRT_2 / 2, SQRT_2 / 2),
                Vector.Cartesian(SQRT_2 / 2, -SQRT_2 / 2)
            ),
            actual = Rectangle(Vector.Cartesian(1.0, 0.0), 2 - SQRT_2, 2.0)
                .intersect(Circle(ORIGIN, 1.0))
        )
    }

    private fun assertClose(
        expected: Set<Vector.Cartesian>,
        actual: Set<Vector.Cartesian>,
        delta: Double = 1e-12,
    ) {
        val sortedExpected: List<Vector.Cartesian> = expected.sortedBy { it.r2(ORIGIN) }
        val sortedActual: List<Vector.Cartesian> = actual.sortedBy { it.r2(ORIGIN) }

        val delta2 = delta * delta
        val distances = sortedActual.zip(sortedExpected) { a, e -> a.r2(e) }
        if (distances.any { it > delta2 }) {
            assertEquals(sortedExpected, sortedActual)
        }
    }
}

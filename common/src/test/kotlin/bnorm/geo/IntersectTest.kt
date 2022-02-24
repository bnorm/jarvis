package bnorm.geo

import bnorm.Vector
import bnorm.r2
import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals

class IntersectTest {
    companion object {
        private val SQRT_2 = sqrt(2.0)
        private val ORIGIN = Vector.Cartesian(0.0, 0.0)
    }

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

    @Test
    fun `test line intersect circle`() {
        assertClose(
            expected = setOf(
                Vector.Cartesian(-SQRT_2 / 2, -SQRT_2 / 2),
                Vector.Cartesian(SQRT_2 / 2, SQRT_2 / 2)
            ),
            actual = Circle(ORIGIN, 1.0) intersect Line(1.0, 0.0)
        )

        assertClose(
            expected = setOf(
                Vector.Cartesian(-1.0, 0.0),
                Vector.Cartesian(0.0, 1.0),
            ),
            actual = Circle(ORIGIN, 1.0) intersect Line(1.0, 1.0)
        )

        assertClose(
            expected = setOf(
                Vector.Cartesian(1.0, 0.0),
                Vector.Cartesian(0.0, -1.0),
            ),
            actual = Circle(ORIGIN, 1.0) intersect Line(1.0, -1.0)
        )

        assertClose(
            expected = setOf(
                Vector.Cartesian(1.0, 0.0),
                Vector.Cartesian(0.0, 1.0),
            ),
            actual = Circle(ORIGIN, 1.0) intersect Line(-1.0, 1.0)
        )

        assertClose(
            expected = setOf(
                Vector.Cartesian(-1.0, 0.0),
                Vector.Cartesian(0.0, -1.0),
            ),
            actual = Circle(ORIGIN, 1.0) intersect Line(-1.0, -1.0)
        )

        // Vertical line
        assertClose(
            expected = setOf(
                Vector.Cartesian(0.0, 1.0),
                Vector.Cartesian(0.0, -1.0),
            ),
            actual = Circle(ORIGIN, 1.0) intersect Line(Double.NaN, 0.0)
        )

        // Horizontal line
        assertClose(
            expected = setOf(
                Vector.Cartesian(1.0, 0.0),
                Vector.Cartesian(-1.0, 0.0)
            ),
            actual = Circle(ORIGIN, 1.0) intersect Line(0.0, 0.0)
        )
    }
}

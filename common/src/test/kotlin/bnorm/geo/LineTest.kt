package bnorm.geo

import bnorm.Vector
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs

class LineTest {
    @Test
    fun `slope and elevation`() {
        val line = Line(Vector.Cartesian(1.0, 3.0), Vector.Cartesian(2.0, 5.0))
        assertEquals(2.0, line.m)
        assertEquals(1.0, line.b)
    }

    @Test
    fun `angle line`() {
//        assertNear(2.0, Line(Vector.Cartesian(0.0, 0.0), 1 * PI / 8).m)
        assertNear(1.0, Line(Vector.Cartesian(0.0, 0.0), 2 * PI / 8).m)
//        assertNear(0.5, Line(Vector.Cartesian(0.0, 0.0), 3 * PI / 8).m)
        assertNear(0.0, Line(Vector.Cartesian(0.0, 0.0), 4 * PI / 8).m)
        assertNear(-1.0, Line(Vector.Cartesian(0.0, 0.0), 3 * PI / 4).m)
        assertNear(1.0, Line(Vector.Cartesian(0.0, 0.0), 5 * PI / 4).m)
        assertNear(-1.0, Line(Vector.Cartesian(0.0, 0.0), 7 * PI / 4).m)
    }

    private fun assertNear(n1: Double, n2: Double, delta: Double = 1e-12) {
        if (abs(n1 - n2) > delta) {
            assertEquals(n1, n2)
        }
    }
}

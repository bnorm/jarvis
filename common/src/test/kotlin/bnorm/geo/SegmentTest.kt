package bnorm.geo

import bnorm.Cartesian
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SegmentTest {

    @Test
    fun `segment contains point`() {
        assertTrue(Cartesian(1.0, 0.0) in Segment(Cartesian(0.0, 0.0), Cartesian(2.0, 0.0)))
        assertTrue(Cartesian(0.0, 1.0) in Segment(Cartesian(0.0, 0.0), Cartesian(0.0, 2.0)))
        assertTrue(Cartesian(-1.0, 0.0) in Segment(Cartesian(0.0, 0.0), Cartesian(-2.0, 0.0)))
        assertTrue(Cartesian(0.0, -1.0) in Segment(Cartesian(0.0, 0.0), Cartesian(0.0, -2.0)))

        assertTrue(Cartesian(1.0, 1.0) in Segment(Cartesian(0.0, 0.0), Cartesian(2.0, 2.0)))
        assertTrue(Cartesian(-1.0, 1.0) in Segment(Cartesian(0.0, 0.0), Cartesian(-2.0, 2.0)))
        assertTrue(Cartesian(-1.0, -1.0) in Segment(Cartesian(0.0, 0.0), Cartesian(-2.0, -2.0)))
        assertTrue(Cartesian(1.0, -1.0) in Segment(Cartesian(0.0, 0.0), Cartesian(2.0, -2.0)))
    }

    @Test
    fun `segments intersect`() {
        assertClose(
            expected = Cartesian(0.0, 0.0),
            actual = Segment(p1 = Cartesian(-1.0, -1.0), p2 = Cartesian(1.0, 1.0)) intersect
                Segment(p1 = Cartesian(-1.0, 1.0), p2 = Cartesian(1.0, -1.0))
        )
        assertClose(
            expected = Cartesian(0.0, 0.0),
            actual = Segment(p1 = Cartesian(-1.0, 1.0), p2 = Cartesian(1.0, -1.0)) intersect
                Segment(p1 = Cartesian(-1.0, -1.0), p2 = Cartesian(1.0, 1.0))
        )

        assertNull(
            Segment(p1 = Cartesian(-1.0, 0.0), p2 = Cartesian(0.0, 1.0)) intersect
                Segment(p1 = Cartesian(0.0, 0.0), p2 = Cartesian(1.0, -1.0))
        )
        assertNull(
            Segment(p1 = Cartesian(0.0, 0.0), p2 = Cartesian(1.0, -1.0)) intersect
                Segment(p1 = Cartesian(-1.0, 0.0), p2 = Cartesian(0.0, 1.0))
        )
    }
}

package bnorm.kdtree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KdTreeTest {
    data class Point(val x: Double, val y: Double) {
        val dimensions: DoubleArray = doubleArrayOf(x, y)
    }

    @Test
    fun neighborSequenceOrder() {
        val pointScales = doubleArrayOf(1.0, 1.0)
        val tree = KdTree(pointScales, 1, Point::dimensions)
        tree.add(Point(1.0, 1.0))
        tree.add(Point(1.0, -1.0))
        tree.add(Point(-1.0, -1.0))
        tree.add(Point(-1.0, 1.0))

        val neighbors = tree.neighbors(Point(0.2, 1.0)).toList()
        assertEquals(
            listOf(Point(1.0, 1.0), Point(-1.0, 1.0), Point(1.0, -1.0), Point(-1.0, -1.0)),
            neighbors.map { it.value },
            neighbors.joinToString { "${it.value}:${it.dist}" }
        )
    }

    @Test
    fun neighborListOrder() {
        val pointScales = doubleArrayOf(1.0, 1.0)
        val tree = KdTree(pointScales, 1, Point::dimensions)
        tree.add(Point(1.0, 1.0))
        tree.add(Point(1.0, -1.0))
        tree.add(Point(-1.0, -1.0))
        tree.add(Point(-1.0, 1.0))

        val neighbors = tree.neighbors(Point(0.2, 1.0), 3).map { it.value }.toSet()
        assertEquals(
            setOf(Point(1.0, 1.0), Point(-1.0, 1.0), Point(1.0, -1.0)),
            neighbors
        )
    }
}

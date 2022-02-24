package bnorm.kdtree

import bnorm.sqr
import bnorm.rollingVariance
import java.util.*
import kotlin.collections.ArrayDeque

class KdTree<T>(
    private val dimensionScales: DoubleArray,
    bucketSize: Int = DEFAULT_BUCKET_SIZE,
    private val dimensionsFunction: (T) -> DoubleArray,
) {
    init {
        require(dimensionScales.isNotEmpty())
    }

    companion object {
        // TODO adjust bucket size
        const val DEFAULT_BUCKET_SIZE = 100
    }

    private class TreePoint<out T>(
        val value: T,
        val dimensions: DoubleArray,
        var dist: Double = 0.0
    ) : Comparable<TreePoint<*>> {
        override fun compareTo(other: TreePoint<*>): Int = compareValues(dist, other.dist)
    }

    private sealed class Node<T> {
        abstract fun add(value: TreePoint<T>): Node<T>

        class Leaf<T>(
            dimensionsCount: Int,
            private val parentDimension: Int,
            val bucket: Array<TreePoint<T>?>
        ) : Node<T>() {
            var bucketCount = 0

            private val means = DoubleArray(dimensionsCount) { 0.0 }
            private val variances = DoubleArray(dimensionsCount) { 0.0 }

            override fun add(value: TreePoint<T>): Node<T> {
                if (bucketCount == bucket.size) return split().add(value)

                bucket[bucketCount] = value
                bucketCount++
                rollingVariance(bucketCount, means, variances, value.dimensions)
                return this
            }

            private fun split(): Branch<T> {
                val dimension = bestSplitDimension()
                bucket.sortBy { it!!.dimensions[dimension] }

                val middle = bucket.size / 2
                val pivot = bucket[middle]!!.dimensions[dimension]

                var up = middle
                var down = middle
                // TODO walk together to avoid excessively long walks in a single direction
                while (up + 1 < bucket.size && bucket[up + 1]!!.dimensions[dimension] == pivot) up++
                while (down - 1 > 0 && bucket[down - 1]!!.dimensions[dimension] == pivot) down--
                val listSplit = if (up - middle > middle - down) down else up

                val right = arrayOfNulls<TreePoint<T>>(bucket.size)
                bucket.copyInto(right, 0, listSplit, bucket.size)

                val left = bucket
                left.fill(null, fromIndex = listSplit)
                return Branch(
                    pivot,
                    dimension,
                    Leaf(variances.size, dimension, left).also {
                        it.bucketCount = listSplit
                        for (i in 1..it.bucketCount) {
                            rollingVariance(i, it.means, it.variances, it.bucket[i - 1]!!.dimensions)
                        }
                    },
                    Leaf(variances.size, dimension, right).also {
                        it.bucketCount = bucket.size - listSplit
                        for (i in 1..it.bucketCount) {
                            rollingVariance(i, it.means, it.variances, it.bucket[i - 1]!!.dimensions)
                        }
                    },
                )
            }

            fun bestSplitDimension(): Int {
                var index = (parentDimension + 1) % variances.size
                var best = variances[index]

                for (i in variances.indices) {
                    if (i == index || i == parentDimension) continue

                    val s = variances[i]
                    if (s > best) {
                        best = s
                        index = i
                    }
                }

                return index
            }

            override fun toString(): String = "Leaf(bucket=$bucket)"
        }

        class Branch<T>(
            val pivot: Double,
            val dimension: Int,
            var left: Node<T>,
            var right: Node<T>,
        ) : Node<T>() {
            override fun add(value: TreePoint<T>): Node<T> {
                if (value.dimensions[dimension] <= pivot) {
                    left = left.add(value)
                } else {
                    right = right.add(value)
                }
                return this
            }

            override fun toString(): String = "Branch(pivot=$pivot,dimension=$dimension)"
        }
    }

    private var root: Node<T> = Node.Leaf(dimensionScales.size, -1, arrayOfNulls(bucketSize))

    fun add(value: T) {
        val neighbor = TreePoint(value, dimensionsFunction(value))
        root = root.add(neighbor)
    }

    data class Neighbor<out T>(
        val value: T,
        val dist: Double,
    ) : Comparable<Neighbor<*>> {
        override fun compareTo(other: Neighbor<*>): Int = compareValues(dist, other.dist)
    }

    private fun TreePoint<T>.toNeighbor(): Neighbor<T> = Neighbor(value, dist)

    fun neighbors(point: T, size: Int): Collection<Neighbor<T>> {
        val pointDimensions = dimensionsFunction(point)
        val pointDimensionsCopy = pointDimensions.copyOf()

        val stack = ArrayDeque<Node<T>>()
        val heap = PriorityQueue<TreePoint<T>>(size, compareBy { -it.dist })

        stack.addLast(root)
        while (stack.isNotEmpty()) {
            when (val node = stack.removeLast()) {
                is Node.Leaf -> {
                    for (i in 0 until node.bucketCount) {
                        val value = node.bucket[i]!!
                        value.dist = dist(pointDimensions, value.dimensions)
                        if (heap.size < size || value.dist < heap.peek().dist) {
                            if (heap.size >= size) heap.poll()
                            heap.add(value)
                        }
                    }
                }
                is Node.Branch -> {
                    if (heap.size < size || dist(pointDimensions, pointDimensionsCopy, node) < heap.peek().dist) {
                        if (pointDimensions[node.dimension] <= node.pivot) {
                            stack.addLast(node.right)
                            stack.addLast(node.left)
                        } else {
                            stack.addLast(node.left)
                            stack.addLast(node.right)
                        }
                    }
                }
            }
        }

        return heap.map { it.toNeighbor() }
    }

    fun neighbors(point: T): Sequence<Neighbor<T>> = sequence {
        val pointDimensions = dimensionsFunction(point)
        val pointDimensionsCopy = pointDimensions.copyOf()

        class Other<T>(
            val node: Node<T>,
            val dist: Double,
        ) : Comparable<Other<T>> {
            override fun compareTo(other: Other<T>): Int = compareValues(dist, other.dist)
        }

        val others = PriorityQueue<Other<T>>()
        val queue = PriorityQueue<TreePoint<T>>()

        fun traverse(start: Node<T>) {
            var node = start
            while (true) {
                node = when (node) {
                    is Node.Leaf -> {
                        for (i in 0 until node.bucketCount) {
                            val value = node.bucket[i]!!
                            value.dist = dist(pointDimensions, value.dimensions)
                            queue.add(value)
                        }
                        return
                    }
                    is Node.Branch -> {
                        val dist = dist(pointDimensions, pointDimensionsCopy, node)
                        if (pointDimensions[node.dimension] <= node.pivot) {
                            others.add(Other(node.right, dist))
                            node.left
                        } else {
                            others.add(Other(node.left, dist))
                            node.right
                        }
                    }
                }
            }
        }

        traverse(root)
        while (others.isNotEmpty()) {
            val other = others.poll()

            var best: TreePoint<T>? = queue.peek()
            while (best != null && other.dist > best.dist) {
                yield(queue.remove().toNeighbor())
                best = queue.peek()
            }

            traverse(other.node)
        }

        while (queue.isNotEmpty()) {
            yield(queue.poll().toNeighbor())
        }
    }

    private fun dist(p1: DoubleArray, p2: DoubleArray): Double {
        var sum = 0.0
        for (i in dimensionScales.indices) {
            sum += sqr(dimensionScales[i] * (p1[i] - p2[i]))
        }
        return sum
    }

    private fun dist(p: DoubleArray, copy: DoubleArray, node: Node.Branch<T>): Double {
        copy[node.dimension] = node.pivot
        val result = dist(p, copy)
        copy[node.dimension] = p[node.dimension]
        return result
    }

    fun print() {
        fun recurse(node: Node.Branch<*>, indent: String) {
            println("${indent}${indent.length / 2}:BRANCH: ${node.dimension}=${node.pivot}")
            (node.left as? Node.Branch)?.let { left ->
                println("${indent}${indent.length / 2}:LEFT")
                recurse(left, "$indent  ")
            }
            (node.right as? Node.Branch)?.let { right ->
                println("${indent}${indent.length / 2}:RIGHT")
                recurse(right, "$indent  ")
            }
        }
        println("ROOT")
        (root as? Node.Branch)?.let { recurse(it, "  ") }
        println()
    }
}

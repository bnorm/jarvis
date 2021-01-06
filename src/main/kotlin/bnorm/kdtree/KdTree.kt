package bnorm.kdtree

import bnorm.sqr
import bnorm.rollingVariance
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.collections.ArrayList

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
        override val value: T,
        val dimensions: DoubleArray,
        override var dist: Double = 0.0
    ) : Neighbor<T> {
        override fun compareTo(other: Neighbor<*>): Int = compareValues(dist, other.dist)
    }

    private sealed class Node<T> {
        abstract fun add(value: TreePoint<T>): Node<T>

        class Leaf<T>(
            dimensionsCount: Int,
            private val parentDimension: Int,
            private val bucketSize: Int,
        ) : Node<T>() {
            val bucket = ArrayList<TreePoint<T>>(DEFAULT_BUCKET_SIZE + 10) // TODO actual array to avoid copy?

            private val means = DoubleArray(dimensionsCount) { 0.0 }
            private val variances = DoubleArray(dimensionsCount) { 0.0 }

            override fun add(value: TreePoint<T>): Node<T> {
                bucket.add(value)
                rollingVariance(bucket.size, means, variances, value.dimensions)
                return if (bucket.size > DEFAULT_BUCKET_SIZE) split() else this
            }

            private fun split(): Branch<T> {
                val dimension = bestSplitDimension()
                bucket.sortBy { it.dimensions[dimension] }

                val middle = bucket.size / 2
                val pivot = bucket[middle].dimensions[dimension]

                var up = middle
                var down = middle
                // TODO walk together to avoid excessively long walks in a single direction
                while (up + 1 < bucket.size && bucket[up + 1].dimensions[dimension] == pivot) up++
                while (down - 1 > 0 && bucket[down - 1].dimensions[dimension] == pivot) down--
                val listSplit = if (up - middle > middle - down) down else up

                val left = bucket.subList(0, listSplit)
                val right = bucket.subList(listSplit, bucket.size)
                return Branch(
                    pivot,
                    dimension,
                    Leaf<T>(variances.size, dimension, bucketSize).also { left.forEach(it::add) },
                    Leaf<T>(variances.size, dimension, bucketSize).also { right.forEach(it::add) },
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

    private var root: Node<T> = Node.Leaf(dimensionScales.size, -1, bucketSize)

    fun add(value: T) {
        val neighbor = TreePoint(value, dimensionsFunction(value))
        root = root.add(neighbor)
    }

    interface Neighbor<out T> : Comparable<Neighbor<*>> {
        val value: T
        val dist: Double
    }

    fun neighbors(point: T, size: Int): Collection<Neighbor<T>> {
        val pointDimensions = dimensionsFunction(point)
        val pointDimensionsCopy = pointDimensions.copyOf()

        val stack = ArrayDeque<Node<T>>()
        val heap = PriorityQueue<TreePoint<T>>(size, compareBy { -it.dist })

        stack.addLast(root)
        while (stack.isNotEmpty()) {
            when (val node = stack.removeLast()) {
                is Node.Leaf -> {
                    for (value in node.bucket) {
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

        return heap
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
                        for (value in node.bucket) {
                            value.dist = dist(pointDimensions, value.dimensions)
                        }
                        queue.addAll(node.bucket)
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
                yield(queue.remove())
                best = queue.peek()
            }

            traverse(other.node)
        }

        while (queue.isNotEmpty()) {
            yield(queue.poll())
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

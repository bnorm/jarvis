package bnorm.kdtree

import bnorm.sqr
import java.util.*
import kotlin.collections.ArrayList

class KdTree<T>(
    // TODO faster way than storing dimension functions
    dimensionsCount: Int,
    private val dimensions: (T) -> DoubleArray,
    private val dist: (DoubleArray, DoubleArray) -> Double
) {
    constructor(vararg dimensions: (T) -> Double, dist: (DoubleArray, DoubleArray) -> Double) : this(
        dimensionsCount = dimensions.size,
        dimensions = { point ->
            DoubleArray(dimensions.size) { index ->
                dimensions[index](point)
            }
        },
        dist = dist
    )

    init {
        require(dimensionsCount > 0)
    }

    companion object {
        private const val BUCKET_SIZE = 100
    }

    private class Dimensional<out T>(
        val point: T,
        val dimensions: DoubleArray,
    )

    private sealed class Node<D : Dimensional<*>> {
        abstract fun add(value: D): Node<D>

        class Leaf<D : Dimensional<*>>(
            dimensionsCount: Int
        ) : Node<D>() {
            private val _bucket = ArrayList<D>(BUCKET_SIZE + 10) // TODO actual array to avoid copy?
            val bucket: List<D> get() = _bucket

            private val means = MutableList(dimensionsCount) { 0.0 }
            private val variances = MutableList(dimensionsCount) { 0.0 }

            override fun add(value: D): Node<D> {
                _bucket.add(value)
                updateVariance(value)

                // TODO adjust bucket size
                return if (_bucket.size > BUCKET_SIZE) split() else this
            }

            private fun updateVariance(value: D): Int {
                val n = _bucket.size
                val dimensions = value.dimensions

                if (n == 1) {
                    for (i in variances.indices) {
                        val d = dimensions[i]
                        val oldMean = means[i]
                        means[i] = (d + (n - 1) * oldMean) / n
                    }
                } else {
                    for (i in variances.indices) {
                        val d = dimensions[i]
                        val oldMean = means[i]
                        means[i] = (d + (n - 1) * oldMean) / n
                        variances[i] = (n - 2) * variances[i] / (n - 1) + sqr(d - oldMean) / n
                    }
                }

                return n
            }

            private fun split(): Branch<D> {
                println("SPLIT! $variances")

                var dimension = -1
                var max = Double.MIN_VALUE

                for (i in 0 until variances.size) {
                    val s = variances[i]
                    if (s > max) {
                        max = s
                        dimension = i
                    }
                }

                _bucket.sortBy { it.dimensions[dimension] }

                val middle = _bucket.size / 2
                val pivot = _bucket[middle].dimensions[dimension]

                var up = middle
                var down = middle
                // TODO walk together to avoid excessively long walks in a single direction
                while (up + 1 < _bucket.size && _bucket[up + 1].dimensions[dimension] == pivot) up++
                while (down - 1 > 0 && _bucket[down - 1].dimensions[dimension] == pivot) down--
                val listSplit = if (up - middle > middle - down) down else up

                val left = _bucket.subList(0, listSplit)
                val right = _bucket.subList(listSplit, _bucket.size)
                return Branch(
                    pivot,
                    dimension,
                    Leaf<D>(variances.size).also { left.forEach(it::add) },
                    Leaf<D>(variances.size).also { right.forEach(it::add) },
                )
            }

            override fun toString(): String = "Leaf(bucket=$bucket)"
        }

        class Branch<D : Dimensional<*>>(
            val pivot: Double,
            val dimension: Int,
            var left: Node<D>,
            var right: Node<D>,
        ) : Node<D>() {
            fun dimensions(base: DoubleArray) = DoubleArray(base.size) { if (it == dimension) pivot else base[it] }

            override fun add(value: D): Node<D> {
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

    private var root: Node<Dimensional<T>> = Node.Leaf(dimensionsCount)

    fun add(value: T) {
        root = root.add(value.toDimensional())
    }

    data class Neighbor<out T>(
        val value: T,
        val dist: Double,
    )

    fun neighbors(point: T): Sequence<Neighbor<T>> = sequence {
        val dimensional = point.toDimensional()
//        println("POINT: ${ dimensional.dimensions.joinToString() }")

        class Other<D : Dimensional<*>>(
            val dist: Double,
            val node: Node<D>
        )

        val others = PriorityQueue<Other<Dimensional<T>>>(compareBy { it.dist })
        val queue = PriorityQueue<Neighbor<T>>(3 * BUCKET_SIZE, compareBy { it.dist })

        fun traverse(start: Node<Dimensional<T>>) {
            var node = start
            while (true) {
                node = when (node) {
                    is Node.Leaf -> {
                        for (value in node.bucket) {
                            val dist = dist(dimensional.dimensions, value.dimensions)
                            queue.add(Neighbor(value.point, dist))
                        }
//                        println("LEAF: first=${queue.firstOrNull()?.dist} last=${queue.lastOrNull()?.dist}")
                        return
                    }
                    is Node.Branch -> {
                        val value = dimensional.dimensions[node.dimension]
                        val pivot = node.pivot
                        val dist = dist(dimensional.dimensions, node.dimensions(dimensional.dimensions))
                        if (value <= pivot) {
//                            println("LEFT: -> pivot=$dist")
                            others.add(Other(dist, node.right))
                            node.left
                        } else {
//                            println("RIGHT: -> pivot=$dist")
                            others.add(Other(dist, node.left))
                            node.right
                        }
                    }
                }
            }
        }

        traverse(root)
        while (others.isNotEmpty()) {
            val other: Other<Dimensional<T>> = others.poll()
//            println("OTHER: dist=${other.dist}")

            var best: Neighbor<T>? = queue.peek()
            while (best != null && other.dist > best.dist) {
                yield(queue.remove())
                best = queue.peek()
            }

            traverse(other.node)
        }

        var best: Neighbor<T>? = queue.poll()
        while (best != null) {
            yield(best)
            best = queue.poll()
        }
    }

    private fun T.toDimensional() = Dimensional(this, dimensions(this))

    fun print() {
//        fun recurse(node: Node.Branch<*>, indent: String) {
//            println("${indent}BRANCH: ${node.dimension}=${node.pivot}")
//            (node.left as? Node.Branch)?.let { left ->
//                println("${indent}LEFT")
//                recurse(left, "$indent  ")
//            }
//            (node.right as? Node.Branch)?.let { right ->
//                println("${indent}RIGHT")
//                recurse(right, "$indent  ")
//            }
//        }
//        println("ROOT")
//        (root as? Node.Branch)?.let { recurse(it, "  ") }
//        println()
    }
}

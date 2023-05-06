//package bnorm.robot
//
//import bnorm.Cartesian
//import bnorm.Polar
//import bnorm.limit
//import TANK_ACCELERATION
//import TANK_DECELERATION
//import TANK_MAX_SPEED
//import bnorm.r2
//import bnorm.signMul
//import bnorm.theta
//import bnorm.toPolar
//import robocode.Rules
//import kotlin.experimental.ExperimentalTypeInference
//import kotlin.math.PI
//import kotlin.math.abs
//import kotlin.math.acos
//import kotlin.math.sqrt
//import kotlin.time.ExperimentalTime
//import kotlin.time.measureTime
//
//fun interpolate(start: RobotScan, end: RobotScan): List<RobotScan>? {
//    val missing = end.time - start.time - 1
//
//    if (missing == 0L) return listOf(start, end)
//    if (missing > 1) return null
//
//
//    val location = end.location - end.velocity
//    val velocity = (location - start.location).toPolar()
//
//    val firstTurn = velocity.theta - start.velocity.theta
//    if (abs(firstTurn) > Rules.getTurnRateRadians(start.velocity.r) + 1e-10) return null
//
//    val secondTurn = end.velocity.theta - velocity.theta
//    if (abs(secondTurn) > Rules.getTurnRateRadians(velocity.r) + 1e-10) return null
//
//    return listOf(
//        start,
//        start.copy(
//            location = location,
//            velocity = velocity,
//            time = start.time + 1
//        ),
//        end
//    )
//}
//
//@OptIn(ExperimentalTime::class)
//fun main() {
//    val permutations = listOf(1, 2, 3).subLists()
//    for (permutation in permutations) {
//        println(permutation)
//    }
//
//
//    println(measureTime {
//        println("possibilities:")
//        interpolateSpeed(7.0, 8.0, 1).forEach {
//            println(it)
//        }
//    })
//
//    println(measureTime {
//        var location = Cartesian(0.0, 0.0)
//        var velocity = Polar(0.0, 5.0)
//        println("location=$location velocity=$velocity")
//
//        val start = RobotScan(location, velocity, 100.0, 0, false)
//
//        velocity = Polar(velocity.theta + Rules.getTurnRateRadians(velocity.r), 6.0)
//        location += velocity
//        println("location=$location velocity=$velocity")
//
//        val expected1 = RobotScan(location, velocity, 100.0, 1, false)
//
//        velocity = Polar(velocity.theta + Rules.getTurnRateRadians(velocity.r), 7.0)
//        location += velocity
//        println("location=$location velocity=$velocity")
//
//        val expected2 = RobotScan(location, velocity, 100.0, 2, false)
//
//        velocity = Polar(velocity.theta + Rules.getTurnRateRadians(velocity.r), 8.0)
//        location += velocity
//        println("location=$location velocity=$velocity")
//
//        val end = RobotScan(location, velocity, 100.0, 3, false)
//
//        run {
//            val guesses = middle(start, expected2, 1).toList()
//            println("guesses = ${guesses}")
//            println("guesses.size = ${guesses.size}")
//            for (guess in guesses) {
//                println("   expected1 = ${expected1}")
//                println("   actual1 = ${guess.get(1)}")
//            }
//        }
//        run {
//            val guesses = middle(start, end, 2).toList()
//            println("guesses = ${guesses}")
//            println("guesses.size = ${guesses.size}")
//            for (guess in guesses) {
//                println("   expected1 = ${expected1}")
//                println("   actual1 = ${guess.get(1)}")
//                println("   expected2 = ${expected2}")
//                println("   actual2 = ${guess.get(2)}")
//            }
//        }
//    })
//}
//
//fun middle(start: RobotScan, end: RobotScan, missing: Int): Sequence<List<RobotScan>> {
//    if (missing == 0) return sequenceOf(listOf(start, end))
//    else if (missing == 1) {
//        val location = end.location - end.velocity
//        val velocity = (location - start.location).toPolar()
//
//        val firstTurn = velocity.theta - start.velocity.theta
//        if (abs(firstTurn) > Rules.getTurnRateRadians(start.velocity.r) + 1e-10) return emptySequence()
//
//        val secondTurn = end.velocity.theta - velocity.theta
//        if (abs(secondTurn) > Rules.getTurnRateRadians(velocity.r) + 1e-10) return emptySequence()
//
//        return sequenceOf(
//            listOf(
//                start,
//                start.copy(
//                    location = location,
//                    velocity = velocity,
//                    time = start.time + 1
//                ),
//                end
//            )
//        )
//    } else if (missing == 2) {
//        val p = end.location - end.velocity
//        val c2 = start.location.r2(p)
//        val c = sqrt(c2)
//
//        val possible = interpolateSpeed(start.velocity.r, end.velocity.r, missing)
//        return possible.transform { speeds ->
//            val (_, a, b, _) = speeds
//            if (a + b < c) TODO("a=$a b=$b c=$c")
//
//            val a2 = a * a
//            val b2 = b * b
//
//            // law of cosines
//            // c2 = a2 + b2 − 2ab cos(C)
//            // b2 = a2 + c2 − 2ac cos(B)
//
//            val C = acos((a2 + b2 - c2) / (2 * a * b))
//            val B = acos((a2 + c2 - b2) / (2 * a * c))
//
//            val theta = start.location.theta(p)
//
//            val h0 = start.velocity.theta
//            val h3 = end.velocity.theta
//
//            fun calc(h1: Double, h2: Double): List<RobotScan>? {
//                if (abs(h1 - h0) > Rules.getTurnRateRadians(start.velocity.r) + 1e-10) return null
//                if (abs(h2 - h1) > Rules.getTurnRateRadians(a) + 1e-10) return null
//                if (abs(h3 - h2) > Rules.getTurnRateRadians(b) + 1e-10) return null
//
//                val aV = Polar(h1, a)
//                val bV = Polar(h2, b)
//
//                return listOf(
//                    start,
//                    start.copy(
//                        location = start.location + aV,
//                        velocity = aV,
//                        time = start.time + 1
//                    ),
//                    start.copy(
//                        location = p,
//                        velocity = bV,
//                        time = start.time + 2
//                    ),
//                    end
//                )
//            }
//
//            calc(theta + B, theta + B + (C - PI))?.let { yield(it) }
//            calc(theta - B, theta - B + (PI - C))?.let { yield(it) }
//        }
//    }
//    TODO()
//}
//
//@OptIn(ExperimentalTypeInference::class)
//fun <T, R> Sequence<T>.transform(
//    @BuilderInference block: SequenceScope<R>.(value: T) -> Unit
//): Sequence<R> = sequence {
//    forEach { value ->
//        block(value)
//    }
//}
//
//@OptIn(ExperimentalTypeInference::class)
//fun <T, R> Sequence<T>.transformIndexed(
//    @BuilderInference block: SequenceScope<R>.(index: Int, value: T) -> Unit
//): Sequence<R> = sequence {
//    forEachIndexed { index, value ->
//        block(index, value)
//    }
//}
//
//@OptIn(ExperimentalStdlibApi::class)
//fun <T> List<T>.subLists(): Sequence<List<T>> {
//    val recursion = DeepRecursiveFunction<List<T>, Sequence<ArrayDeque<T>>> { values ->
//        if (values.isEmpty()) sequenceOf(ArrayDeque(this@subLists.size))
//        else {
//            val last = values[values.size - 1]
//            callRecursive(values.subList(0, values.size - 1)).transform {
//                it.addLast(last)
//                yield(it)
//                it.removeLast()
//                yield(it)
//            }
//        }
//    }
//
//    return recursion(this)
//}
//
//fun interpolateSpeed(start: Double, end: Double, missing: Int): Sequence<List<Double>> {
//    fun recurse(start: Double, end: Double, missing: Int): Sequence<ArrayDeque<Double>> {
//        if (missing == 0) {
//            // Decelerate
//            // -8 .. -6 -> 2
//            // 8 .. 6 -> -2
//
//            // Accelerate
//            // 6 .. 7 -> 1
//            // -6 .. -7 -> -1
//
//            val change = signMul(start) * (end - start)
//            return if (change in -TANK_DECELERATION..TANK_ACCELERATION) {
//                val deque = ArrayDeque<Double>(missing)
//                deque.addFirst(end)
//                deque.addFirst(start)
//                sequenceOf(deque)
//            } else {
//                emptySequence()
//            }
//        } else {
//            return sequence {
//                yieldAll(recurse(start, end, missing - 1))
//
//                val accelerate = limit(-TANK_MAX_SPEED, start + signMul(start) * TANK_ACCELERATION, TANK_MAX_SPEED)
//                if (start != accelerate) {
//                    yieldAll(recurse(accelerate, end, missing - 1))
//                }
//
//                if (start != 0.0) {
//                    val decelerate = limit(-TANK_MAX_SPEED, start - signMul(start) * TANK_DECELERATION, TANK_MAX_SPEED)
//                    yieldAll(recurse(decelerate, end, missing - 1))
//                }
//            }.transform {
//                it.addFirst(start)
//                yield(it)
//                it.removeFirst()
//            }
//        }
//    }
//
//    return recurse(start, end, missing)
//}
//
//private operator fun <T> T.plus(elements: List<T>): List<T> {
//    val result = ArrayList<T>(elements.size + 1)
//    result.add(this)
//    result.addAll(elements)
//    return result
//}
//
//fun <T> List<T>.permutations(): Sequence<List<T>> {
//    fun <T> SequenceScope<List<T>>.recursive(values: MutableList<T>, size: Int) {
//        fun swap(i: Int, j: Int) {
//            val temp = values[i]
//            values[i] = values[j]
//            values[j] = temp
//        }
//
//        // if size becomes 1 then yield the obtained permutation
//        if (size == 1) yield(values)
//        for (i in 0 until size) {
//            recursive(values, size - 1)
//
//            if (size % 2 == 1) {
//                // if size is odd, swap first and last element
//                swap(0, size - 1)
//            } else {
//                // If size is even, swap i and last element
//                swap(i, size - 1)
//            }
//        }
//    }
//
//    return sequence { recursive(toMutableList(), size) }
//}

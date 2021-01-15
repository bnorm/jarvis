package bnorm.neural

import org.junit.jupiter.api.Test
import kotlin.math.abs

class NeuralNetworkTest {
    private fun assertNear(actual: Double, expected: Double, delta: Double) {
        val actualDelta = abs(actual - expected)
        if (actualDelta > abs(delta)) {
            throw AssertionError("Difference between $actual and $expected is greater than $delta: $actualDelta")
        }
    }

    private fun assertNear(actual: DoubleArray, expected: DoubleArray, delta: Double) {
        for (i in actual.indices) {
            val actualDelta = abs(actual[i] - expected[i])
            if (actualDelta > abs(delta)) {
                throw AssertionError("Difference between ${actual.toList()} and ${expected.toList()} at $i is greater than $delta: $actualDelta")
            }
        }
    }

    private fun NeuralNetwork.run(vararg values: Double): Double {
        for (i in values.indices) {
            input[i] = values[i]
        }
        forward()
        return output[0]
    }

    private fun NeuralNetwork.train(vararg values: Double, expected: Double): Double {
        val actual = run(*values)
        error[0] = expected - actual
        train(0.5)
        return actual
    }

    @Test
    fun `or neural network`() = with(NeuralNetwork(2, 1, biased = true)) {
        repeat(1000) {
            train(0.0, 0.0, expected = 0.0)
            train(1.0, 0.0, expected = 1.0)
            train(0.0, 1.0, expected = 1.0)
            train(1.0, 1.0, expected = 1.0)
        }

        assertNear(run(0.0, 0.0), 0.0, 0.1)
        assertNear(run(1.0, 0.0), 1.0, 0.1)
        assertNear(run(0.0, 1.0), 1.0, 0.1)
        assertNear(run(1.0, 1.0), 1.0, 0.1)
    }

    @Test
    fun `and neural network`() = with(NeuralNetwork(2, 1, biased = true)) {
        repeat(1000) {
            train(0.0, 0.0, expected = 0.0)
            train(1.0, 0.0, expected = 0.0)
            train(0.0, 1.0, expected = 0.0)
            train(1.0, 1.0, expected = 1.0)
        }

        assertNear(run(0.0, 0.0), 0.0, 0.1)
        assertNear(run(1.0, 0.0), 0.0, 0.1)
        assertNear(run(0.0, 1.0), 0.0, 0.1)
        assertNear(run(1.0, 1.0), 1.0, 0.1)
    }

    @Test
    fun `xor neural network`() = with(NeuralNetwork(2, 8, 1, biased = true)) {
        repeat(1000) {
            train(0.0, 0.0, expected = 0.0)
            train(1.0, 0.0, expected = 1.0)
            train(0.0, 1.0, expected = 1.0)
            train(1.0, 1.0, expected = 0.0)
        }

        assertNear(run(0.0, 0.0), 0.0, 0.1)
        assertNear(run(1.0, 0.0), 1.0, 0.1)
        assertNear(run(0.0, 1.0), 1.0, 0.1)
        assertNear(run(1.0, 1.0), 0.0, 0.1)
    }

    @Test
    fun `nor neural network`() = with(NeuralNetwork(2, 1, biased = true)) {
        repeat(1000) {
            train(0.0, 0.0, expected = 1.0)
            train(1.0, 0.0, expected = 0.0)
            train(0.0, 1.0, expected = 0.0)
            train(1.0, 1.0, expected = 0.0)
        }

        assertNear(run(0.0, 0.0), 1.0, 0.1)
        assertNear(run(1.0, 0.0), 0.0, 0.1)
        assertNear(run(0.0, 1.0), 0.0, 0.1)
        assertNear(run(1.0, 1.0), 0.0, 0.1)
    }

    @Test
    fun `nand neural network`() = with(NeuralNetwork(2, 1, biased = true)) {
        repeat(1000) {
            train(0.0, 0.0, expected = 1.0)
            train(1.0, 0.0, expected = 1.0)
            train(0.0, 1.0, expected = 1.0)
            train(1.0, 1.0, expected = 0.0)
        }

        assertNear(run(0.0, 0.0), 1.0, 0.1)
        assertNear(run(1.0, 0.0), 1.0, 0.1)
        assertNear(run(0.0, 1.0), 1.0, 0.1)
        assertNear(run(1.0, 1.0), 0.0, 0.1)
    }

    @Test
    fun `bucket neural network`() = with(NeuralNetwork(1, 10, 5, biased = true)) {
        fun runM(value: Double): DoubleArray {
            input[0] = value
            forward()
            return output
        }

        fun trainM(value: Double, expected: DoubleArray): DoubleArray {
            val actual = runM(value)
            for (i in expected.indices) {
                error[i] = expected[i] - actual[i]
            }
            train(0.5)
            return actual
        }

        val b1 = doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0)
        val b2 = doubleArrayOf(0.0, 1.0, 0.0, 0.0, 0.0)
        val b3 = doubleArrayOf(0.0, 0.0, 1.0, 0.0, 0.0)
        val b4 = doubleArrayOf(0.0, 0.0, 0.0, 1.0, 0.0)
        val b5 = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 1.0)
        repeat(10000) {
            trainM(0.0, expected = b1)
            trainM(0.25, expected = b2)
            trainM(0.5, expected = b3)
            trainM(0.75, expected = b4)
            trainM(1.0, expected = b5)
        }

        assertNear(runM(0.0), b1, 0.1)
        assertNear(runM(0.25), b2, 0.1)
        assertNear(runM(0.5), b3, 0.1)
        assertNear(runM(0.75), b4, 0.1)
        assertNear(runM(1.0), b5, 0.1)
    }
}

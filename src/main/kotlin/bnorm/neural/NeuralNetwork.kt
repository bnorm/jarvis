package bnorm.neural

import bnorm.sqr
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

interface Activation {
    operator fun invoke(v: Double): Double
    fun derivative(v: Double): Double

    abstract class ConstantDerivative(
        val derivative: Double
    ) : Activation {
        final override fun derivative(v: Double): Double = derivative
    }

    companion object {
        val Linear = object : ConstantDerivative(1.0) {
            override fun invoke(v: Double): Double = v
        }

        val Sigmoid = object : Activation {
            override fun invoke(v: Double): Double = 1.0 / (1.0 + exp(-v))
            override fun derivative(v: Double): Double {
                val value = invoke(v)
                return (value * (1 - value)).coerceAtLeast(2.2204460492503126E-16)
            }
        }

        val Gaussian = object : Activation {
            override fun invoke(v: Double): Double = exp(-sqr(v))
            override fun derivative(v: Double): Double {
                val derivative = -2 * v * invoke(v)
                val nonZeroPositiveDerivative = abs(derivative).coerceAtLeast(1.354304923E-315)
                return nonZeroPositiveDerivative * sign(derivative)
            }
        }

        val Sine = object : Activation {
            override fun invoke(v: Double): Double = sin(v)
            override fun derivative(v: Double): Double = cos(v)
        }

        val Tanh = object : Activation {
            override fun invoke(v: Double): Double = tanh(v)
            override fun derivative(v: Double): Double = (1 - sqr(invoke(v))).coerceAtLeast(2.220446049250313E-16)
        }

        val SmoothMax = object : Activation {
            override fun invoke(v: Double): Double = ln(1 + exp(v))
            override fun derivative(v: Double): Double {
                val e = exp(v)
                return e / (e + 1)
            }
        }

        val ReLU = object : Activation {
            override fun invoke(v: Double): Double = 0.0.coerceAtLeast(v)
            override fun derivative(v: Double): Double = if (v <= 0.0) 0.0 else v
        }
    }
}

class NeuralNetwork(
    vararg layers: Int,
    private val activation: Activation = Activation.Sigmoid,
    biased: Boolean = false,
    // TODO include layer index
    initWeight: (inputNode: Int, outputNode: Int) -> Double = { _, _ ->
        Random.nextDouble(-1.0, 1.0)
    }
) {
    init {
        require(layers.size > 1)
    }

    class Layer(
        val inputs: Int,
        val outputs: Int,
        val biased: Boolean,
        initWeight: (inputNode: Int, outputNode: Int) -> Double
    ) {
        val columns = inputs + if (biased) 1 else 0
        val weights = DoubleArray(outputs * columns) { i -> initWeight(i, i) }
        val output = DoubleArray(outputs)
        val h = DoubleArray(outputs)
    }

    val input = DoubleArray(layers.first())
    val layers = layers.toList().windowed(2)
        .map { (inputs, outputs) -> Layer(inputs, outputs, biased, initWeight) }
    val output = this.layers.last().output

    // Feed the *already configured input* through the neural network
    fun forward() {
        var input = input
        for (layer in layers) {
            layer.forward(input)
            input = layer.output
        }
    }

    private fun Layer.forward(input: DoubleArray) {
        for (o in 0 until outputs) {
            var sum = 0.0
            if (biased) sum += weights[o * columns + inputs] * 1.0
            for (i in 0 until inputs) {
                sum += weights[o * columns + i] * input[i]
            }
            output[o] = activation.invoke(sum)
        }
    }

    // propagate the error through the neural network
    // NOTE: the output array is assumed to be configured to be the error
    fun train(alpha: Double) {
        for (l in layers.lastIndex downTo 0) {
            val input = if (l == 0) input else layers[l - 1].output
            layers[l].train(alpha, input)
        }
    }

    private fun Layer.train(alpha: Double, input: DoubleArray) {
        for (o in 0 until outputs) {
            var delta = 0.0
            if (biased) delta += weights[o * columns + inputs] * 1.0
            for (i in 0 until inputs) {
                delta += weights[o * columns + i] * input[i]
            }
            h[o] = activation.derivative(delta)
        }

        if (biased) {
            for (o in 0 until outputs) {
                weights[o * columns + inputs] = weights[o * columns + inputs] + alpha * output[o] * h[0]
            }
        }

        for (i in 0 until inputs) {
            var error = 0.0
            for (o in 0 until outputs) {
                val w = weights[o * columns + i]
                error += w * output[o]
                weights[o * columns + i] = w + alpha * input[i] * output[o] * h[o]
            }
            input[i] = error
        }
    }
}

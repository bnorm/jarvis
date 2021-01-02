package bnorm.neural

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
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
            override fun invoke(v: Double): Double = exp(-v.pow(2.0))
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
            override fun derivative(v: Double): Double = (1 - invoke(v).pow(2.0)).coerceAtLeast(2.220446049250313E-16)
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
    private val activation: Activation = Activation.Sine,
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
        val weights = Array(outputs) { outputNode ->
            DoubleArray(inputs + if (biased) 1 else 0) { inputNode -> initWeight(inputNode, outputNode) }
        }
        val output = DoubleArray(outputs)
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
            val w = weights[o]
            for (i in 0 until inputs) {
                sum += w[i] * input[i]
            }
            if (biased) sum += w[w.lastIndex] * 1.0
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
        val h = DoubleArray(outputs) { derivative(it, input) }

        if (biased) {
            for (o in 0 until outputs) {
                weights[o][inputs] = weights[o][inputs] + alpha * 1.0 * output[o] * derivative(o, input)
            }
        }

        for (i in 0 until inputs) {
            var error = 0.0
            for (o in 0 until outputs) {
                val w = weights[o][i]
                error += w * output[o]
                weights[o][i] = w + alpha * input[i] * output[o] * h[o]
            }
            input[i] = error
        }
    }

    private fun Layer.derivative(output: Int, input: DoubleArray): Double {
        if (activation is Activation.ConstantDerivative) return activation.derivative

        val w = weights[output]
        var h = 0.0
        for (i in 0 until inputs) {
            h += w[i] * input[i]
        }
        if (biased) h += w[inputs] * 1.0
        return activation.derivative(h)
    }
}

//fun main() {
////    val weights = listOf(1.0, 1.0, -0.5, -1.0, -1.0, +1.5, 1.0, 1.0, -1.5).iterator()
//    val network = NeuralNetwork(2, 4, 1, biased = true) { _, _ ->
//        Random.nextDouble(0.5, 1.5)
////        weights.next()
//    }
//
//    network.printLayers()
//
//    println(run(network, 0.0, 0.0))
//    println(run(network, 1.0, 0.0))
//    println(run(network, 0.0, 1.0))
//    println(run(network, 1.0, 1.0))
//
//    repeat(100) {
//        train(network, 0.0, 0.0, 0.0)
//        train(network, 1.0, 0.0, 1.0)
//        train(network, 0.0, 1.0, 1.0)
//        train(network, 1.0, 1.0, 0.0)
//    }
//
//    network.printLayers()
//
//    println(run(network, 0.0, 0.0))
//    println(run(network, 1.0, 0.0))
//    println(run(network, 0.0, 1.0))
//    println(run(network, 1.0, 1.0))
//}
//
//private fun train(network: NeuralNetwork, i1: Double, i2: Double, expected: Double): Double {
//    val actual = run(network, i1, i2)
//    network.output[0] = expected - actual
//    network.backprop(0.5)
//    return actual
//}
//
//private fun run(network: NeuralNetwork, i1: Double, i2: Double): Double {
//    network.input[0] = i1
//    network.input[1] = i2
//    network.forward()
//    return network.output[0]
//}
//
//private fun NeuralNetwork.printLayers() {
//    for ((l, layer) in layers.withIndex()) {
//        println("layer ${l + 1}")
//        for (weight in layer.weights) {
//            println(weight.toList())
//        }
//    }
//}

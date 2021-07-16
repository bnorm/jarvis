package bnorm.neural

import kotlin.random.Random

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
        val error = DoubleArray(outputs)
    }

    val input = DoubleArray(layers.first())
    val layers = layers.toList().windowed(2)
        .map { (inputs, outputs) -> Layer(inputs, outputs, biased, initWeight) }
    val output = this.layers.last().output
    val error = this.layers.last().error

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
    // NOTE: it is assumed the network has already been run forward
    // NOTE: the error array is assumed to be configured to be the error
    fun train(alpha: Double) {
        for (l in layers.lastIndex downTo 0) {
            val input = if (l == 0) input else layers[l - 1].output
            val inputError = if (l == 0) input else layers[l - 1].error
            layers[l].train(alpha, input, inputError)
        }
    }

    private fun Layer.train(alpha: Double, input: DoubleArray, inputError: DoubleArray) {
        for (o in 0 until outputs) {
            output[o] = alpha * derivative(o, input) * error[o]
        }

        if (biased) {
            for (o in 0 until outputs) {
                weights[o * columns + inputs] = weights[o * columns + inputs] + output[o]
            }
        }

        for (i in 0 until inputs) {
            var sum = 0.0
            for (o in 0 until outputs) {
                val w = weights[o * columns + i]
                sum += w * error[o]
                weights[o * columns + i] = w + input[i] * output[o]
            }
            inputError[i] = sum
        }
    }

    private fun Layer.derivative(outputNode: Int, input: DoubleArray): Double {
        if (activation is Activation.OutputActivation) {
            return activation.outputDerivative(output[outputNode])
        }

        var delta = 0.0
        if (biased) delta += weights[outputNode * columns + inputs] * 1.0
        for (i in 0 until inputs) {
            delta += weights[outputNode * columns + i] * input[i]
        }
        return activation.derivative(delta)
    }
}

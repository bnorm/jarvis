package bnorm.neural

import bnorm.sqr
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.tanh

interface Activation {
    operator fun invoke(input: Double): Double
    fun derivative(input: Double): Double

    // If the activation derivative can be calculated from the the output
    interface OutputActivation : Activation {
        override fun derivative(input: Double): Double = outputDerivative(invoke(input))
        fun outputDerivative(output: Double): Double
    }

    companion object {
        val Linear = object : OutputActivation {
            override fun invoke(input: Double): Double = input
            override fun outputDerivative(output: Double): Double = 1.0
        }

        val Sigmoid = object : OutputActivation {
            override fun invoke(input: Double): Double = 1.0 / (1.0 + exp(-input))
            override fun outputDerivative(output: Double): Double =
                (output * (1 - output))
        }

        val Gaussian = object : Activation {
            override fun invoke(input: Double): Double = exp(-sqr(input))
            override fun derivative(input: Double): Double {
                val derivative = -2 * input * invoke(input)
                val nonZeroPositiveDerivative = abs(derivative)
                return nonZeroPositiveDerivative * sign(derivative)
            }
        }

        val Sine = object : Activation {
            override fun invoke(input: Double): Double = sin(input)
            override fun derivative(input: Double): Double = cos(input)
        }

        val Tanh = object : OutputActivation {
            override fun invoke(input: Double): Double = tanh(input)
            override fun outputDerivative(output: Double): Double =
                (1 - sqr(output))
        }

        val SmoothMax = object : Activation {
            override fun invoke(input: Double): Double = ln(1 + exp(input))
            override fun derivative(input: Double): Double {
                val e = exp(input)
                return e / (e + 1)
            }
        }

        val ReLU = object : OutputActivation {
            override fun invoke(input: Double): Double = minOf(input, 0.0)
            override fun outputDerivative(output: Double): Double = output
        }
    }
}

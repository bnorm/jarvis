package bnorm.parts.gun

import bnorm.Polar
import bnorm.Vector
import bnorm.neural.NeuralNetwork
import bnorm.neural.printLayers
import bnorm.robot.Robot
import bnorm.robot.RobotSnapshots
import bnorm.roundDecimals
import bnorm.theta
import robocode.Rules
import kotlin.time.measureTime

class NeuralGuessFactorPrediction<T : GuessFactorSnapshot>(
    private val self: Robot,
    private val neuralNetwork: NeuralNetwork,
    private val prediction: GuessFactorPrediction<T>,
    private val dimensionsFunction: (T) -> DoubleArray,
) : Prediction {
    override fun predict(robot: Robot, bulletPower: Double): Vector {
        val escapeAngle = escapeAngle(Rules.getBulletSpeed(bulletPower))
//        val distance = r(gun.x, gun.y, robot.latest.location)
//        val robotAngle = robotAngle(distance)

        val heading = theta(self.latest.location, robot.latest.location)
        val rotationDirection = robot.context[RobotSnapshots].latest.rotateDirection // TODO

//        neuralNetwork.printLayers()
        val buckets: DoubleArray
        println("prediction " + measureTime {
            buckets = buckets()
        })
        println(buckets.toList())

        var max = 0.0
        var index = (buckets.size - 1) / 2
        for (i in buckets.indices) {
            val value = buckets[i]
            if (value > max) {
                max = value
                index = i
            }
        }

        val gf = index.toGuessFactor(buckets.size)
        return Polar(heading + rotationDirection * gf * escapeAngle, 1.0)
    }

    private fun buckets(): DoubleArray {
        val actual = prediction.latestWave.snapshot
        val actualValues = dimensionsFunction(actual)
        require(neuralNetwork.input.size == actualValues.size * 2 + 1)

        val output = DoubleArray(neuralNetwork.output.size)
        for (neighbor in prediction.latestWave.cluster) {
            forward(actualValues, neighbor.value, dimensionsFunction(neighbor.value))

            for (i in output.indices) {
                output[i] += neuralNetwork.output[i] / (1 + neighbor.dist)
            }
        }

        return output
    }

    fun train(actual: T, neighbors: List<T>, guessFactor: Double) {
        val actualValues = dimensionsFunction(actual)
        require(neuralNetwork.input.size == actualValues.size * 2 + 1)

        val bucket = guessFactor.toBucket(neuralNetwork.output.size)
        for (neighbor in neighbors) {
            forward(actualValues, neighbor, dimensionsFunction(neighbor))

            for (i in neuralNetwork.output.indices) {
                if (i == bucket) {
                    neuralNetwork.output[i] = 1.0 - neuralNetwork.output[i]
                } else {
                    neuralNetwork.output[i] = 0.0 - neuralNetwork.output[i]
                }
            }

            neuralNetwork.train(0.1)
        }
    }

    private fun forward(actualValues: DoubleArray, neighbor: T, neighborValues: DoubleArray) {
        neuralNetwork.input[0] = neighbor.guessFactor
        for (i in actualValues.indices) {
            neuralNetwork.input[1 + i] = neighborValues[i]
            neuralNetwork.input[1 + i + actualValues.size] = actualValues[i]
        }
        neuralNetwork.forward()
    }
}

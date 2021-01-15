package bnorm.parts.gun

import bnorm.Polar
import bnorm.Vector
import bnorm.kdtree.KdTree
import bnorm.neural.NeuralNetwork
import bnorm.parts.gun.virtual.escapeAngle
import bnorm.robot.Robot
import bnorm.robot.snapshot
import bnorm.theta
import robocode.Rules
import kotlin.time.measureTime

class NeuralGuessFactorPrediction<T : GuessFactorSnapshot>(
    private val self: Robot,
    private val neuralNetwork: NeuralNetwork,
    private val clusterFunction: (Robot) -> Pair<T, Collection<KdTree.Neighbor<T>>>,
    private val dimensionsFunction: (T) -> DoubleArray,
) : Prediction {
    override suspend fun predict(robot: Robot, bulletPower: Double): Vector {
        val escapeAngle = escapeAngle(self, robot, Rules.getBulletSpeed(bulletPower))
//        val distance = r(gun.x, gun.y, robot.latest.location)
//        val robotAngle = robotAngle(distance)

        val heading = theta(self.latest.location, robot.latest.location)
        val rotationDirection = robot.snapshot.rotateDirection

//        neuralNetwork.printLayers()
        val buckets: DoubleArray
        println("prediction " + measureTime {
            buckets = buckets(robot)
        })
//        println(buckets.toList())

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
        val bearing = rotationDirection * gf * if (gf < 0) escapeAngle.reverse else escapeAngle.forward
        return Polar(heading + bearing, 1.0)
    }

    private fun buckets(robot: Robot): DoubleArray {
        val (actual, cluster) = clusterFunction(robot)
        val actualValues = dimensionsFunction(actual)
        require(neuralNetwork.input.size == actualValues.size * 2 + 1)

        val output = DoubleArray(neuralNetwork.output.size)
        for (neighbor in cluster) {
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
                    neuralNetwork.error[i] = 1.0 - neuralNetwork.output[i]
                } else {
                    neuralNetwork.error[i] = 0.0 - neuralNetwork.output[i]
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

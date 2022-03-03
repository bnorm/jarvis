package bnorm.parts.gun

import bnorm.Polar
import bnorm.Vector
import bnorm.geo.times
import bnorm.kdtree.KdTree
import bnorm.neural.NeuralNetwork
import bnorm.parts.tank.escape.escape
import bnorm.robot.Robot
import bnorm.robot.snapshot
import bnorm.theta
import robocode.Rules

class NeuralGuessFactorPrediction<T : GuessFactorSnapshot>(
    private val self: Robot,
    private val robot: Robot,
    private val neuralNetwork: NeuralNetwork,
    private val clusterFunction: (Robot) -> Pair<T, Collection<KdTree.Neighbor<T>>>,
    private val dimensionsFunction: (T) -> DoubleArray,
) : Prediction {
    override fun predict(bulletPower: Double): Vector {
        val source = self.latest.location
        val target = robot.latest.location
        val escapeAngle = self.battleField.escape(source, target, Rules.getBulletSpeed(bulletPower))
//        val distance = r(gun.x, gun.y, robot.latest.location)
//        val robotAngle = robotAngle(distance)

        val heading = theta(source, target)
        val direction = robot.snapshot.gfDirection

//        neuralNetwork.printLayers()
        val buckets: DoubleArray
//        println("prediction " + measureTime {
            buckets = buckets(robot)
//        })
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

        val gf = direction * index.toGuessFactor(buckets.size)
        val bearing = gf * if (gf < 0) escapeAngle.leftAngle else escapeAngle.rightAngle
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

    fun train(actual: T, neighbors: Collection<KdTree.Neighbor<T>>, guessFactor: Double = actual.guessFactor) {
        val actualValues = dimensionsFunction(actual)
        require(neuralNetwork.input.size == actualValues.size * 2 + 1)

        val bucket = guessFactor.toBucket(neuralNetwork.output.size)
        for (neighbor in neighbors) {
            forward(actualValues, neighbor.value, dimensionsFunction(neighbor.value))

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

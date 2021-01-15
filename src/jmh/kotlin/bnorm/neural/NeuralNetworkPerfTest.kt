package bnorm.neural

import org.openjdk.jmh.Main
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

@Fork(1)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class NeuralNetworkPerfTest {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = Main.main(arrayOf(NeuralNetworkPerfTest::class.java.name))
    }

    @Param("4")
    var layerCount: Int = 0

    @Param("1000")
    var nodeCount: Int = 0

    @Param("Linear", "Sigmoid", "Gaussian", "Sine", "Tanh", "SmoothMax", "ReLU")
    lateinit var activation: String

    lateinit var network: NeuralNetwork

    @Setup
    fun setup() {
        network = NeuralNetwork(
            *IntArray(layerCount) { nodeCount },
            activation = when (activation) {
                "Linear" -> Activation.Linear
                "Sigmoid" -> Activation.Sigmoid
                "Gaussian" -> Activation.Gaussian
                "Sine" -> Activation.Sine
                "Tanh" -> Activation.Tanh
                "SmoothMax" -> Activation.SmoothMax
                "ReLU" -> Activation.ReLU
                else -> error("unknown=$activation")
            },
        )
    }

    @Benchmark
    fun forward() = network.forward()

    @Benchmark
    fun train() = network.train(0.1)
}

/*
Benchmark                                      (activation)  (layers)  (nodeCount)    Mode     Cnt       Score      Error  Units
NeuralNetworkPerfTest.forward                        Linear         4         1000  sample    2529    3960.155 ±   14.508  us/op
NeuralNetworkPerfTest.forward                       Sigmoid         4         1000  sample    2482    4031.479 ±   14.159  us/op
NeuralNetworkPerfTest.forward                      Gaussian         4         1000  sample    2320    4317.871 ±   18.532  us/op
NeuralNetworkPerfTest.forward                          Sine         4         1000  sample    2517    3984.012 ±   13.952  us/op
NeuralNetworkPerfTest.forward                          Tanh         4         1000  sample    2496    4011.806 ±   13.421  us/op
NeuralNetworkPerfTest.forward                     SmoothMax         4         1000  sample    2431    4123.329 ±   15.660  us/op
NeuralNetworkPerfTest.forward                          ReLU         4         1000  sample     744   13483.415 ±  194.702  us/op

Benchmark                                      (activation)  (layers)  (nodeCount)    Mode     Cnt       Score      Error  Units
NeuralNetworkPerfTest.train                          Linear         4         1000  sample     212   47525.347 ±  814.392  us/op
NeuralNetworkPerfTest.train                         Sigmoid         4         1000  sample     194   52520.010 ± 1441.613  us/op
NeuralNetworkPerfTest.train                        Gaussian         4         1000  sample     204   49444.663 ±  467.092  us/op
NeuralNetworkPerfTest.train                            Sine         4         1000  sample     204   49787.121 ±  281.406  us/op
NeuralNetworkPerfTest.train                            Tanh         4         1000  sample     204   49763.027 ±  605.277  us/op
NeuralNetworkPerfTest.train                       SmoothMax         4         1000  sample     205   49934.915 ±  225.140  us/op
NeuralNetworkPerfTest.train                            ReLU         4         1000  sample     210   48639.259 ±  245.416  us/op
 */

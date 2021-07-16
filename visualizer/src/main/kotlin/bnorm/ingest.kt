package bnorm

import bnorm.debug.GuessFactorSnapshot
import bnorm.kdtree.KdTree
import bnorm.parts.gun.toGuessFactor
import bnorm.robot.RobotScan
import bnorm.robot.RobotSnapshot
import bnorm.robot.snapshot.WallProbe
import com.jakewharton.picnic.RowDsl
import com.jakewharton.picnic.table
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.useLines
import kotlin.time.measureTime

fun main() {
    println(measureTime {
        val scales = DoubleArray(RobotSnapshot.DIMENSIONS.size) { 1.0 }
        val tree = KdTree(scales, KdTree.DEFAULT_BUCKET_SIZE, RobotSnapshot::guessFactorDimensions)
        val gfTree = KdTree<RobotSnapshot>(doubleArrayOf(1.0), KdTree.DEFAULT_BUCKET_SIZE) {
            doubleArrayOf(it.guessFactor)
        }

        resource("snapshots-pe.SandboxDT 1.91TC.json").snapshots {
            tree.add(it)
            gfTree.add(it)
        }

        for (i in 0..60) {
            val gf = i.toGuessFactor(61)
            describe(gfTree, guessFactor = gf, limit = 2000)
        }
    })
}

private fun describe(gfTree: KdTree<RobotSnapshot>, guessFactor: Double, limit: Int? = null) {
    val snapshot = blankSnapshot()
    val similar = gfTree.neighbors(snapshot.apply { this.guessFactor = guessFactor })
        .takeWhile { it.dist < 0.001 }

    val means = DoubleArray(RobotSnapshot.DIMENSIONS.size)
    val variances = DoubleArray(RobotSnapshot.DIMENSIONS.size)
    var count = 0
    similar.forEachIndexed { index, neighbor ->
        count = index
        rollingVariance(index, means, variances, neighbor.value.guessFactorDimensions)
    }

    println("count=${count + 1} gf=$guessFactor")
    println(table {
        cellStyle { border = true }
        row { cell("Dimension"); RobotSnapshot.DIMENSIONS.forEach { cell(it.name) } }
        row { cell("Mean"); dimension(means) }
        row { cell("Variance"); dimension(variances) }
    })
    println()
}

private fun RowDsl.dimension(values: DoubleArray) {
    for (v in values) {
        cell((v * 1000).toInt() / 1000.0)
    }
}

fun blankSnapshot() = RobotSnapshot(
    scan = RobotScan(
        location = Vector.Cartesian(0.0, 0.0),
        velocity = Vector.Polar(0.0, 0.0),
        energy = 0.0,
        time = 0,
        interpolated = false
    ),
    moveDirection = 0,
    rotateDirection = 0,
    accelerationDirection = 0,
    wallProbe = WallProbe(
        position = WallProbe.Position(0.0, 0.0, 0.0, 0.0),
        heading = WallProbe.Movement(0.0, 0.0, 0.0),
        perpendicular = WallProbe.Movement(0.0, 0.0, 0.0)
    ),
    distance = 0.0,
    lateralSpeed = 0.0,
    lateralAcceleration = 0.0,
    advancingSpeed = 0.0,
    advancingAcceleration = 0.0,
    acceleration = 0.0,
    distLast10 = 0.0,
    distLast30 = 0.0,
    distLast90 = 0.0,
    timeDeltaMoveDirection = 0,
    timeDeltaRotateDirection = 0,
    timeDeltaAccelerationDirection = 0,
    timeDeltaVelocityChange = 0,
    cornerDistance = 0.0,
    cornerDirection = 0.0,
    activeWaveCount = 0
)

fun resource(path: String): Path = Paths.get(ClassLoader.getSystemResource(path).toURI())

val json = Json { ignoreUnknownKeys = true }

fun Path.snapshots(block: (RobotSnapshot) -> Unit) {
    useLines { lines ->
        lines.forEach {
            block(json.decodeFromString(it))
        }
    }
}

fun Path.dSnapshots(block: (GuessFactorSnapshot) -> Unit) {
    useLines { lines ->
        lines.drop(1).forEach {
            block(json.decodeFromString(it))
        }
    }
}

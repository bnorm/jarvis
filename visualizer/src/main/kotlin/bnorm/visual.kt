package bnorm

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.singleWindowApplication
import bnorm.debug.GuessFactorSnapshot
import bnorm.kdtree.KdTree
import bnorm.parts.gun.gauss
import bnorm.parts.gun.toGuessFactor
import bnorm.robot.RobotSnapshot
import bnorm.ui.GuessFactorGraph
import kotlinx.serialization.decodeFromString
import kotlin.io.path.useLines

val file = resource("snapshots-cx.mini.Cigaret 1.31TC.json")

fun main() = singleWindowApplication(
    title = "Compose for Desktop",
) {
    var snapshots by remember { mutableStateOf<List<GuessFactorSnapshot>?>(null) }
    var tree by remember { mutableStateOf<KdTree<GuessFactorSnapshot>?>(null) }
    var header by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(Unit) {
        header = json.decodeFromString(file.useLines { it.first() })

        val collect = mutableListOf<GuessFactorSnapshot>()
        file.dSnapshots { collect.add(it) }

        if (collect.isNotEmpty()) {
            snapshots = collect

            val dimensionScales = DoubleArray(collect.first().dimensions.size) { 1.0 }
            val kdTree = KdTree<GuessFactorSnapshot>(dimensionScales) { it.dimensions }
            for (snapshot in collect) {
                kdTree.add(snapshot)
            }
            tree = kdTree
        }
    }

    var graph by remember { mutableStateOf<Collection<KdTree.Neighbor<GuessFactorSnapshot>>>(emptyList()) }
    Row {
        GuessFactorGraph(graph)
        DimensionScales(
            dimensions = header,
            onValueChange = {
                val tree = tree
                if (tree != null) {
                    graph = tree.neighbors(GuessFactorSnapshot(it.toDoubleArray(), 0.0), 50)
                }
            }
        )
    }


//    ScrollPane {
//            Row(Modifier, Arrangement.spacedBy(5.dp)) {
//                repeat(dimensions) { column ->
//                    Column(Modifier, Arrangement.spacedBy(5.dp)) {
//                        repeat(dimensions - 1) { row ->
//                            Box(Modifier) {
//                                SnapshotGraph(snapshots, column, dimensions - row - 1)
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
}

@Composable
fun DimensionScales(dimensions: List<String>, onValueChange: (List<Double>) -> Unit) {
    var snapshot by remember(dimensions) { mutableStateOf(List(dimensions.size) { 0.0 }) }
    Column {
        for ((index, name) in dimensions.withIndex()) {
            val value = snapshot[index]
            DimensionScale(
                name = name,
                value = value,
                onValueChange = {
                    val copy = snapshot.toMutableList().apply { this[index] = it }
                    snapshot = copy
                    onValueChange(copy)
                }
            )
        }
    }
}

@Composable
fun DimensionScale(
    name: String,
    value: Double,
    onValueChange: (Double) -> Unit,
) {
    Column {
        Text("$name : $value")
        // TODO graph distribution of snapshots for selected dimension
        Slider(
            value = value.toFloat(),
            valueRange = -1.0f..1.0f,
            onValueChange = { onValueChange(it.toDouble()) }
        )
    }
}

@Composable
fun ScrollPane(
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        val stateVertical = rememberScrollState(0)
        val stateHorizontal = rememberScrollState(0)

        Box(
            modifier = Modifier.fillMaxSize()
                .padding(end = 12.dp, bottom = 12.dp)
                .verticalScroll(stateVertical)
                .horizontalScroll(stateHorizontal),
        ) {
            body()
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd)
                .fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )

        HorizontalScrollbar(
            modifier = Modifier.align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(end = 12.dp),
            adapter = rememberScrollbarAdapter(stateHorizontal)
        )
    }
}


@Composable
fun SnapshotGraph(
    snapshots: List<RobotSnapshot>,
    column: Int,
    row: Int,
    graphSize: Dp = 200.dp,
) {
    val columnName = RobotSnapshot.DIMENSIONS[column].name
    val rowName = RobotSnapshot.DIMENSIONS[row].name
    val color = if (column < row) Color.White else Color.Unspecified

    Column(Modifier.padding(8.dp)) {
        Text("X:$columnName\nY:$rowName", color = color, fontSize = 8.sp)
        Canvas(Modifier.size(graphSize, graphSize).background(color)) {
            if (column < row) {
                for (snapshot in snapshots) {
                    val c = snapshot.guessFactorDimensions[column]
                    val r = snapshot.guessFactorDimensions[row]
                    require(c in 0.0..1.0 && r in 0.0..1.0) { "($columnName:$c, $rowName:$r) " }

                    val green = ((snapshot.guessFactor + 1.0) / 2.0 * 0xFF).toInt()
                    drawCircle(
                        Color(0xFF - green, green, 0, 0xFF / 2),
                        2.0f,
                        Offset((c * size.width).toFloat(), (size.height - r * size.height).toFloat())
                    )
                }

//                // X axis
//                drawLine(Color.Black, Offset(0.0f, size.height / 2.0f), Offset(size.width, size.height / 2.0f))
//                // Y axis
//                drawLine(Color.Black, Offset(size.width / 2.0f, 0.0f), Offset(size.width / 2.0f, size.height))
            }
        }
    }
}

fun Iterable<KdTree.Neighbor<GuessFactorSnapshot>>.buckets(bucketCount: Int): DoubleArray {
    val buckets = DoubleArray(bucketCount)

    for (b in buckets.indices) {
        val gf = b.toGuessFactor(bucketCount)
        for (point in this) {
            buckets[b] += gauss(1.0 / point.dist, point.value.guessFactor, 0.1, gf)
        }
    }

    return buckets
}

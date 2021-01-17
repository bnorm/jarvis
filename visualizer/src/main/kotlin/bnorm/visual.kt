package bnorm

import androidx.compose.desktop.Window
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.ScrollableRow
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import bnorm.robot.RobotSnapshot

val dimensions = RobotSnapshot.DIMENSIONS.size

fun main() = Window(
    title = "Compose for Desktop",
    size = IntSize(800, 800)
) {
    var snapshots by remember { mutableStateOf(listOf<RobotSnapshot>()) }
    LaunchedEffect(Unit) {
        val collect = mutableListOf<RobotSnapshot>()
        resource("snapshots-pe.SandboxDT 1.91TC.json")
            .snapshots { collect.add(it) }
        snapshots = collect.apply { shuffle() }.subList(0, 1_000)
    }
    ScrollPane(Modifier.background(Color.Black).padding(8.dp)) {
        Row(Modifier, Arrangement.spacedBy(5.dp)) {
            repeat(dimensions) { column ->
                Column(Modifier, Arrangement.spacedBy(5.dp)) {
                    repeat(dimensions - 1) { row ->
                        Box(Modifier) {
                            SnapshotGraph(snapshots, column, dimensions - row - 1)
                        }
                    }
                }
            }
        }
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
        val stateVertical = rememberScrollState(0f)
        val stateHorizontal = rememberScrollState(0f)

        ScrollableColumn(
            modifier = Modifier.fillMaxSize()
                .padding(end = 12.dp, bottom = 12.dp),
            scrollState = stateVertical
        ) {
            ScrollableRow(scrollState = stateHorizontal) {
                body()
            }
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
    graphSize: Dp = 200.dp
) {
    val columnName = RobotSnapshot.DIMENSIONS[column].name
    val rowName = RobotSnapshot.DIMENSIONS[row].name
    val color = if (column < row) Color.White else Color.Unspecified

    Column(Modifier.padding(8.dp)) {
        Text("X:$columnName\nY:$rowName", color = color, fontSize = TextUnit.Sp(8))
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

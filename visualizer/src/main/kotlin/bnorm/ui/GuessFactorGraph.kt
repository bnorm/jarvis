package bnorm.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bnorm.buckets
import bnorm.debug.GuessFactorSnapshot
import bnorm.kdtree.KdTree

@Composable
fun GuessFactorGraph(
    neighbors: Collection<KdTree.Neighbor<GuessFactorSnapshot>>,
    graphSize: Dp = 400.dp,
) {
    val bucketCount = 101
    val buckets = neighbors.buckets(bucketCount)
    val max = buckets.maxOrNull() ?: 0.0

    Column(Modifier.padding(8.dp).border(1.dp, Color.Black)) {
        Canvas(Modifier.size(graphSize, graphSize).background(Color.White)) {
            // Center Line
            drawLine(Color.Black, Offset(size.width / 2.0f, 0.0f), Offset(size.width / 2.0f, size.height))

            buckets.asSequence().zipWithNext().forEachIndexed { i, (v1, v2) ->
                val x1 = i / (bucketCount - 1).toFloat()
                val y1 = (v1 / max).toFloat()
                val x2 = (i + 1) / (bucketCount - 1).toFloat()
                val y2 = (v2 / max).toFloat()
                drawLine(
                    Color.Black,
                    Offset(x1 * size.width, size.height - y1 * size.height),
                    Offset(x2 * size.width, size.height - y2 * size.height)
                )
            }
        }
    }
}

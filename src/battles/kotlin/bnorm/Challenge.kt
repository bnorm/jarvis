package bnorm

import com.jakewharton.picnic.BorderStyle
import com.jakewharton.picnic.RowDsl
import com.jakewharton.picnic.TableDsl
import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table

data class Challenge(
    val name: String,
    val sessions: Int,
    val groups: List<Group>
) {
    init {
        require(groups.asSequence().map { it.results }.flatten().map { it.scores.size }.distinct().count() == 1)
    }

    data class Group(
        val name: String,
        val results: List<Result>
    ) {
        init {
            require(results.map { it.scores.size }.distinct().count() == 1)
        }
    }

    data class Result(
        val name: String,
        val scores: List<Double>
    )
}

fun Challenge.toTable() = table {
    style {
        borderStyle = BorderStyle.Hidden
    }
    cellStyle {
        alignment = TextAlignment.MiddleRight
        paddingLeft = 1
        paddingRight = 1
    }
    header {
        row {
            cell(name) {
                alignment = TextAlignment.BottomCenter
                columnSpan = 3 + sessions
            }
        }
        row {
            cellStyle {
                borderBottom = true
                borderTop = true
            }
            cell("Bot") {
                columnSpan = 2
                alignment = TextAlignment.BottomCenter
                borderLeft = true
                borderRight = true
            }
            cell("Score") {
                borderLeft = true
                borderRight = true
            }

            for (i in 0 until sessions) {
                cell("Session ${i + 1}")
            }
        }
    }

    for (group in groups) {
        category(group.name, group.results)
    }

    if (groups.size > 1) {
        row {
            cellStyle {
                borderTop = true
            }
            cell("")

            val results = groups.map { it.results }.flatten()
            val scores = List(results.map { it.scores.size }.maxOrNull()!!) { index ->
                var n = 0
                var sum = 0.0
                for (result in results) {
                    if (result.scores.size > index) {
                        n++
                        sum += result.scores[index]
                    }
                }
                sum / n
            }
            botRow("Average", scores)
        }
    }
}

private fun TableDsl.category(
    title: String,
    results: List<Challenge.Result>
) {
    for ((index, result) in results.withIndex()) {
        val scores = result.scores
        row {
            if (index == 0) {
                cellStyle {
                    borderTop = true
                }
                cell(title) {
                    rowSpan = results.size + if (results.size > 1) 1 else 0
                    borderRight = true
                }
            }
            botRow(result.name, scores)
        }
    }

    if (results.size > 1) {
        row {
            cellStyle {
                borderTop = true
            }
            val scores = List(results.map { it.scores.size }.maxOrNull()!!) { index ->
                var n = 0
                var sum = 0.0
                for (result in results) {
                    if (result.scores.size > index) {
                        n++
                        sum += result.scores[index]
                    }
                }
                sum / n
            }
            botRow("$title Average", scores)
        }
    }
}

private fun RowDsl.botRow(name: String, scores: List<Double>) {
    cell(name) {
        borderRight = true
    }
    val totalScore = scores.sum() / scores.size
    cell(totalScore.roundDecimals(1)) {
        borderLeft = true
        borderRight = true
    }
    for (score in scores) {
        cell(score.roundDecimals(1))
    }
}

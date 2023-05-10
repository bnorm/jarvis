package bnorm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import org.jetbrains.skiko.wasm.onWasmReady

fun main() {
    onWasmReady {
        Window {
            var data by remember { mutableStateOf<BattleData?>(null) }
            LaunchedEffect(Unit) { data = BattleData.get() }

            when (val it = data) {
                null -> Loading()
                else -> Home(it)
            }
        }
    }
}

@Composable
fun Loading() {
    Text("Loading battle data...")
}

@Composable
fun Home(data: BattleData) {
    val challenges = data.commits.flatMap { it.challenges }.toSet()

    Column {
        for (challenge in challenges) {
            Challenge(
                challenge,
                data.commits.filter { commit -> challenge in commit.challenges }
                    .associate { commit -> commit.info to commit.battles.single { it.name == challenge } }
                    .toList()
                    .sortedBy { it.first.dateTime })
        }
    }
}

@Composable
fun Commit(commit: Commit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = modifier.clickable { expanded = !expanded }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("${commit.info.dateTime} - Battles: ${commit.battles.size} - ${commit.info.shortMessage}")

            if (expanded) {
                for (battle in commit.battles) {
                    Text("    " + battle.name + " => " + battle.averageScore)
                }
            }
        }
    }
}


@Composable
fun Challenge(name: String, battles: List<Pair<Commit.Info, Battle>>) {
    Surface {
        Column {
            Text(name)

            Row {
                Text("Commit", modifier = Modifier.width(100.dp))
                Text(" || " + Challenge.KNOWN.getValue(name))
            }
            for ((info, battle) in battles) {
                Row {
                    Text(info.abbreviatedId, modifier = Modifier.width(100.dp)) // TODO make clickable to commit?

                    Text(" || ${
                        battle.groups.joinToString(separator = " || ") { group ->
                            "${group.results.joinToString(separator = " | ") { result -> result.averageScore.round(1) }} || ${
                                group.averageScore.round(1)
                            }"
                        }
                    } || ${battle.averageScore.round(1)}")
                }
            }
        }
    }
}

private fun Double.round(places: Int): String {
    val parts = this.toString().split(".")
    if (places > 0) {
        return parts[0] + "." + parts[1].substring(0, places)
    } else if (places == 0) {
        return parts[0]
    } else {
        error("unable to handle negative numbers")
    }
}

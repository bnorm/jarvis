package bnorm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun BattleExecutor.challenge1v1(
    targetBot: String,
    sessions: Int,
    rounds: Int = 35,
    name: String,
    groups: Map<String, List<String>>
): Challenge = runBlocking {
    val results: Map<String, List<Double>> = groups.values.flatten()
        .asFlow()
        .map(parallelism = Runtime.getRuntime().availableProcessors()) { enemy ->
            enemy to runSessions(targetBot, enemy, sessions, rounds)
        }
        .toList().toMap()

    return@runBlocking Challenge(
        name = name,
        sessions = sessions,
        groups = groups.map { (name, bots) ->
            Challenge.Group(name, bots.map { Challenge.Result(it, results.getValue(it)) })
        }
    )
}

private suspend fun BattleExecutor.runSessions(
    targetBot: String,
    enemy: String,
    sessions: Int,
    rounds: Int
): List<Double> {
    val battle = Battle(rounds = rounds, robots = listOf(targetBot, enemy))
    val scores = mutableListOf<Double>()

    println("Running against $enemy")
    repeat(sessions) { session ->
        val result = run(battle)
            .robots
            .single { it.name == targetBot }

        val score = result.bulletDamage / rounds
        println("Session ${session + 1} : $enemy -> $score")
        scores.add(score)
    }

    println("Final : $enemy -> ${scores.sum() / scores.size}")
    return scores
}

private fun <T, R> Flow<T>.map(parallelism: Int, transform: suspend (value: T) -> R): Flow<R> {
    require(parallelism > 0)

    if (parallelism == 1) {
        return map(transform)
    }

    val upstream = this
    return channelFlow {
        val upstreamChannel = upstream.produceIn(this)
        repeat(parallelism) {
            launch {
                for (value in upstreamChannel) {
                    send(transform(value))
                }
            }
        }
    }
}

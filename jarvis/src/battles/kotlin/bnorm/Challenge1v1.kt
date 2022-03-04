package bnorm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
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
    data class Paring(
        val enemy: String,
        val session: Int,
        val battle: Battle,
    )

    data class Result(
        val enemy: String,
        val session: Int,
        val score: Double,
    )

    val results: Map<String, List<Result>> = groups.values.flatten()
        .asFlow()
        .flatMapConcat { enemy ->
            flow {
                repeat(sessions) {
                    emit(Paring(enemy, it, Battle(rounds = rounds, robots = listOf(targetBot, enemy))))
                }
            }
        }
        .map(parallelism = Runtime.getRuntime().availableProcessors()) { (enemy, session, battle) ->
            val result = run(battle)
                .robots
                .single { it.name == targetBot }

            val score = result.bulletDamage / rounds
            println("Session ${session + 1} : $enemy -> $score")

            Result(enemy, session, score)
        }
        .toList()
        .groupBy { it.enemy }

    return@runBlocking Challenge(
        name = name,
        sessions = sessions,
        groups = groups.map { (group, bots) ->
            Challenge.Group(group, bots.map { bot ->
                Challenge.Result(bot, results.getValue(bot).sortedBy { it.session }.map { it.score })
            })
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

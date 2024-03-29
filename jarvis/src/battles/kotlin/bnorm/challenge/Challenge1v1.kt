package bnorm.challenge

import bnorm.battle.Battle
import bnorm.battle.BattleExecutor
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
        val name: String,
        val session: Int,
        val score: Double,
    )

    val enemies = groups.values.flatten()
    val results: Map<String, List<Result>> = enemies
        .asFlow()
        .flatMapConcat { enemy ->
            flow {
                repeat(sessions) {
                    emit(Paring(enemy, it, Battle(rounds = rounds, robots = listOf(targetBot, enemy))))
                }
            }
        }
        .map(parallelism = Runtime.getRuntime().availableProcessors()) { (enemy, session, battle) ->
            val name = enemy.replace(".jar", "").replace("_", " ")
            val result = run(battle)
                .robots
                .single { it.name == targetBot }

            val score = result.bulletDamage / rounds
            println("Session ${session + 1} : $name -> $score")

            Result(name, session, score)
        }
        .toList()
        .groupBy { it.name }

    return@runBlocking Challenge(
        name = name,
        sessions = sessions,
        groups = groups.mapNotNull { (group, bots) ->
            if (bots.isEmpty()) return@mapNotNull null
            Challenge.Group(group, bots.map { bot ->
                val botName = bot.replace(".jar", "").replace("_", " ")
                Challenge.Result(botName, results.getValue(botName).sortedBy { it.session }.map { it.score })
            })
        }
    )
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

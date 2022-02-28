package bnorm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import robocode.control.IRobocodeEngine

fun RobocodeEnginePool.challenge1v1(
    targetBot: String,
    sessions: Int,
    rounds: Int = 35,
    name: String,
    groups: Map<String, List<String>>
): Challenge = runBlocking(Dispatchers.IO) {
    val results = groups.values.flatten()
        .asFlow()
        // Haven't figured out how to make BattleEngine parallel safe yet
        .map(parallelism = 1) { enemy ->
            val engine = borrow()
            try {
                enemy to engine.runSessions(targetBot, enemy, sessions, rounds)
            } finally {
                engine.close()
            }
        }
        .toList().toMap()

    return@runBlocking Challenge(
        name = name,
        sessions = sessions,
        groups = groups.map { (name, bots) ->
            Challenge.Group(name, results.filterKeys { it in bots }
                .map { (name, scores) -> Challenge.Result(name, scores) })
        }
    )
}

private fun IRobocodeEngine.runSessions(
    targetBot: String,
    enemy: String,
    sessions: Int,
    rounds: Int
): MutableList<Double> {
    val targetBotSpec = getLocalRepository(targetBot).single()
    val enemySpec = getLocalRepository(enemy).single()

    val scores = mutableListOf<Double>()
    val enemyName = enemySpec.nameAndVersion
    println("Running against $enemyName")
    repeat(sessions) { session ->
        val result = runBattle(rounds = rounds, robots = listOf(targetBotSpec, enemySpec))
            .single { it.teamLeaderName == targetBot }
        val score = result.bulletDamage.toDouble() / rounds
        println("Session ${session + 1} : $enemyName -> $score")
        scores.add(score)
    }
    println("Final : $enemyName -> ${scores.sum() / scores.size}")
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

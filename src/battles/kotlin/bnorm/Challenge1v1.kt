package bnorm

import robocode.control.RobocodeEngine

fun RobocodeEngine.challenge1v1(
    targetBot: String,
    sessions: Int,
    rounds: Int = 35,
    name: String,
    groups: Map<String, List<String>>
): Challenge {
    val targetBotSpec = getLocalRepository(targetBot).single()
    val enemies = groups.values.flatten().map { getLocalRepository(it).single() }

    val results = mutableMapOf<String, MutableList<Double>>()
    for (enemy in enemies) {
        val enemyName = enemy.nameAndVersion
        println("Running against $enemyName")
        repeat(sessions) { session ->
            val result = runBattle(rounds = rounds, robots = listOf(targetBotSpec, enemy))
                .single { it.teamLeaderName == targetBot }
            val score = result.bulletDamage.toDouble() / rounds
            println("Session ${session + 1} : $enemyName -> $score")
            results.getOrPut(enemyName) { mutableListOf() }.add(score)
        }
        val scores = results[enemyName]!!
        println("Final : $enemyName -> ${scores.sum() / scores.size}")
    }

    return Challenge(
        name = name,
        sessions = sessions,
        groups = groups.map { (name, bots) ->
            Challenge.Group(name, results.filterKeys { it in bots }
                .map { (name, scores) -> Challenge.Result(name, scores) })
        }
    )
}

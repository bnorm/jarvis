package bnorm.tc

import bnorm.Challenge
import bnorm.runBattle
import robocode.control.RobocodeEngine

fun RobocodeEngine.runTargetChallengeRandomMovement(
    targetBot: String,
    sessions: Int
): Challenge = challenge1v1(
    targetBot = targetBot,
    sessions = sessions,
    name = "Targeting Challenge: Random Movement",
    groups = mapOf(
        "Easy" to listOf(
            "apv.AspidMovement 1.0",
            "dummy.micro.Sparrow 2.5TC",
            "kawigi.mini.Fhqwhgads 1.1TC",
            "emp.Yngwie 1.0",
            "kawigi.sbf.FloodMini 1.4TC",
        ),
        "Medium" to listOf(
            "abc.Tron 2.01",
            "wiki.etc.HTTC 1.0",
            "wiki.etc.RandomMovementBot 1.0",
            "davidalves.micro.DuelistMicro 2.0TC",
            "gh.GrubbmGrb 1.2.4TC",
        ),
        "Hard" to listOf(
            "pe.SandboxDT 1.91TC",
            "cx.mini.Cigaret 1.31TC",
            "kc.Fortune 1.0",
            "simonton.micro.WeeklongObsession 1.5TC",
            "jam.micro.RaikoMicro 1.44TC",
        )
    )
)

fun RobocodeEngine.challenge1v1(
    targetBot: String,
    sessions: Int,
    name: String,
    groups: Map<String, List<String>>
): Challenge {
    val targetBotSpec = getLocalRepository(targetBot).single()
    val enemies = groups.values.flatten().map { getLocalRepository(it).single() }

    val results = mutableMapOf<String, MutableList<Double>>()
    for (enemy in enemies) {
        val enemyName = enemy.nameAndVersion
        println("Running against $enemyName")
        repeat(sessions) { round ->
            val result = runBattle(rounds = 35, robots = listOf(targetBotSpec, enemy))
                .single { it.teamLeaderName == targetBot }
            val score = result.bulletDamage.toDouble() / 35
            println("Session ${round + 1} : $enemyName -> $score")
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

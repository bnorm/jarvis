package bnorm

import robocode.BattleResults
import robocode.control.BattleSpecification
import robocode.control.BattlefieldSpecification
import robocode.control.RobocodeEngine
import robocode.control.RobotSpecification
import robocode.control.events.BattleCompletedEvent

fun RobocodeEngine.runBattle(
    rounds: Int = 35,
    battleField: BattlefieldSpecification = BattlefieldSpecification(800, 600),
    robots: List<RobotSpecification>
): List<BattleResults> {
    val spec = BattleSpecification(
        rounds,
        battleField,
        robots.toTypedArray()
    )

    val listener = MemoryBattleListener()
    addBattleListener(listener)
    runBattle(spec, true)
    removeBattleListener(listener)

    return listener.events
        .filterIsInstance<BattleCompletedEvent>().single()
        .indexedResults.toList()
}

package bnorm

import robocode.control.events.BattleCompletedEvent
import robocode.control.events.BattleErrorEvent
import robocode.control.events.BattleEvent
import robocode.control.events.BattleFinishedEvent
import robocode.control.events.BattleMessageEvent
import robocode.control.events.BattlePausedEvent
import robocode.control.events.BattleResumedEvent
import robocode.control.events.BattleStartedEvent
import robocode.control.events.IBattleListener
import robocode.control.events.RoundEndedEvent
import robocode.control.events.RoundStartedEvent
import robocode.control.events.TurnEndedEvent
import robocode.control.events.TurnStartedEvent

class MemoryBattleListener : IBattleListener {
    val events = mutableListOf<BattleEvent>()
    override fun onBattleStarted(event: BattleStartedEvent) { events.add(event) }
    override fun onBattleFinished(event: BattleFinishedEvent) { events.add(event) }
    override fun onBattleCompleted(event: BattleCompletedEvent) { events.add(event) }
    override fun onBattlePaused(event: BattlePausedEvent) { events.add(event) }
    override fun onBattleResumed(event: BattleResumedEvent) { events.add(event) }
    override fun onRoundStarted(event: RoundStartedEvent) { events.add(event) }
    override fun onRoundEnded(event: RoundEndedEvent) { events.add(event) }
    override fun onTurnStarted(event: TurnStartedEvent) { events.add(event) }
    override fun onTurnEnded(event: TurnEndedEvent) { events.add(event) }
    override fun onBattleMessage(event: BattleMessageEvent) { events.add(event) }
    override fun onBattleError(event: BattleErrorEvent) { events.add(event) }
}

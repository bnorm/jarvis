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
    private val _events = mutableListOf<BattleEvent>()
    val events: List<BattleEvent> get() = _events
    private fun addEvent(event: BattleEvent) {
        _events.add(event)
    }

    override fun onBattleStarted(event: BattleStartedEvent) = addEvent(event)
    override fun onBattleFinished(event: BattleFinishedEvent) = addEvent(event)
    override fun onBattleCompleted(event: BattleCompletedEvent) = addEvent(event)
    override fun onBattlePaused(event: BattlePausedEvent) = addEvent(event)
    override fun onBattleResumed(event: BattleResumedEvent) = addEvent(event)
    override fun onRoundStarted(event: RoundStartedEvent) = addEvent(event)
    override fun onRoundEnded(event: RoundEndedEvent) = addEvent(event)
    override fun onTurnStarted(event: TurnStartedEvent) = addEvent(event)
    override fun onTurnEnded(event: TurnEndedEvent) = addEvent(event)
    override fun onBattleMessage(event: BattleMessageEvent) = addEvent(event)
    override fun onBattleError(event: BattleErrorEvent) = addEvent(event)
}

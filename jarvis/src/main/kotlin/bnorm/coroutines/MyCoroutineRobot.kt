package bnorm.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield
import robocode.BattleEndedEvent
import robocode.BulletHitBulletEvent
import robocode.BulletHitEvent
import robocode.BulletMissedEvent
import robocode.CustomEvent
import robocode.DeathEvent
import robocode.Event
import robocode.HitByBulletEvent
import robocode.HitRobotEvent
import robocode.HitWallEvent
import robocode.RobotDeathEvent
import robocode.RoundEndedEvent
import robocode.ScannedRobotEvent
import robocode.StatusEvent
import robocode.WinEvent

typealias RobotTurn = suspend (events: Flow<Event>) -> Unit

abstract class MyCoroutineRobot : CoroutineRobot() {
    private val roundJob = SupervisorJob()
    val roundScope = CoroutineScope(roundJob + Computation)

    private val _events = Channel<Event>(capacity = Channel.UNLIMITED)

    final override suspend fun coroutineRun() {
        val turnEvents = flow {
            while (true) {
                emit(_events.tryReceive().getOrNull() ?: return@flow)
            }
        }

        val turn = init()
        yield()
        while (true) {
            turn.invoke(turnEvents)
            yield()
            execute()
        }
    }

    abstract suspend fun init(): RobotTurn

    private fun send(event: Event) {
        _events.trySendBlocking(event)
    }

    final override fun onBattleEnded(event: BattleEndedEvent) = send(event)
    final override fun onBulletHit(event: BulletHitEvent) = send(event)
    final override fun onBulletHitBullet(event: BulletHitBulletEvent) = send(event)
    final override fun onBulletMissed(event: BulletMissedEvent) = send(event)
    final override fun onCustomEvent(event: CustomEvent) = send(event)
    final override fun onDeath(event: DeathEvent) = send(event)
    final override fun onHitByBullet(event: HitByBulletEvent) = send(event)
    final override fun onHitRobot(event: HitRobotEvent) = send(event)
    final override fun onHitWall(event: HitWallEvent) = send(event)
    final override fun onRobotDeath(event: RobotDeathEvent) = send(event)
    final override fun onScannedRobot(event: ScannedRobotEvent) = send(event)
    final override fun onStatus(event: StatusEvent) = send(event)
    final override fun onWin(event: WinEvent) = send(event)
    final override fun onRoundEnded(event: RoundEndedEvent) {
        roundScope.cancel()
        send(event)
    }
}

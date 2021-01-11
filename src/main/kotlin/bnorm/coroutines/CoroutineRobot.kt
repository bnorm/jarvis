package bnorm.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import robocode.AdvancedRobot
import robocode.BattleEndedEvent
import robocode.BulletHitBulletEvent
import robocode.BulletHitEvent
import robocode.DeathEvent
import robocode.Event
import robocode.HitByBulletEvent
import robocode.RobotDeathEvent
import robocode.RoundEndedEvent
import robocode.ScannedRobotEvent
import robocode.StatusEvent
import robocode.WinEvent
import kotlin.concurrent.thread

typealias RobotTurn = suspend (events: Flow<Event>) -> Unit

abstract class CoroutineRobot : AdvancedRobot() {
    // Better: Executors.newFixedThreadPool(4).asCoroutineDispatcher()
    // But throws a RuntimePermission exception on executor shutdown
    private val _computation = QueueCoroutineDispatcher()
    val Computation: CoroutineDispatcher get() = _computation
    private val threads = List(4) {
        thread(name = "Computation $it") {
            _computation.execute()
        }
    }

    private lateinit var _main: CoroutineDispatcher
    val Main: CoroutineDispatcher get() = _main

    final override fun run() = runBlocking {
        _main = coroutineContext[CoroutineDispatcher]!!

        val eventFlow = flow<Event> {
            var event = events.poll()
            while (event != null) {
                emit(event)
                event = events.poll()
            }
        }

        try {
            val turn = init()
            while (true) {
                turn.invoke(eventFlow)
                execute()
            }
        } finally {
            _computation.close()
        }
    }

    abstract suspend fun init(): RobotTurn

    private val events = Channel<Event>(capacity = Channel.UNLIMITED)
    private fun add(event: Event) {
        events.offer(event)
    }

    override fun onStatus(event: StatusEvent) = add(event)
    override fun onScannedRobot(event: ScannedRobotEvent) = add(event)
    override fun onRobotDeath(event: RobotDeathEvent) = add(event)
    override fun onBulletHit(event: BulletHitEvent) = add(event)
    override fun onHitByBullet(event: HitByBulletEvent) = add(event)
    override fun onBulletHitBullet(event: BulletHitBulletEvent) = add(event)
    override fun onWin(event: WinEvent) = add(event)
    override fun onDeath(event: DeathEvent) = add(event)
    override fun onRoundEnded(event: RoundEndedEvent) = add(event)
}

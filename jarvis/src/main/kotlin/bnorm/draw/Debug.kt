package bnorm.draw

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.awt.Graphics2D

enum class DebugKey(
    val enabled: Boolean
) {
    MinimumRiskMovement(false),
    OrbitMovement(false),
    WallSmoothMovement(false),
    SimulateMovement(false),
    AttackWaves(false),
    TargetEscapeEnvelope(false),
    InterpolatedMovement(true),
}

object Debug {
    data class DrawAction(
        val key: DebugKey,
        val action: (Graphics2D) -> Unit,
    )

    var enabled = false

    private val _queue = MutableSharedFlow<DrawAction>(extraBufferCapacity = Channel.UNLIMITED)
    val drawActions = _queue.asSharedFlow()

    @PublishedApi
    internal fun queueDrawAction(key: DebugKey, block: Graphics2D.() -> Unit) {
        _queue.tryEmit(DrawAction(key, block))
    }

    inline fun onDraw(key: DebugKey, crossinline block: Graphics2D.() -> Unit) {
        if (enabled) queueDrawAction(key) { block() }
    }

    fun onPrint(block: () -> Unit) {
        if (enabled) block()
    }
}

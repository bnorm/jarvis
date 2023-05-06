package bnorm.robot

import bnorm.parts.BattleField
import bnorm.plugin.Context
import bnorm.plugin.ContextHolder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

typealias ScanListener = (scan: RobotScan) -> Unit
typealias DeathListener = () -> Unit

class Robot constructor(
    val name: String,
    override val context: Context,
    val battleField: BattleField
) : ContextHolder {
    companion object {
        fun create(
            name: String,
            context: Context,
            battleField: BattleField,
            initial: RobotScan
        ): Robot {
            val robot = Robot(name, context, battleField)
            robot.revive(initial)
            return robot
        }
    }

    private val scanListeners = mutableListOf<ScanListener>()
    private val deathListeners = mutableListOf<DeathListener>()

    private var _latest: RobotScan? = null
    val latest: RobotScan get() = _latest!!

    private val _snapshots = MutableSharedFlow<RobotScan>(replay = 1, extraBufferCapacity = 100_000)
    val snapshots = _snapshots.asSharedFlow()

    val history: Sequence<RobotScan> get() = generateSequence(latest) { it.prev }

    fun scan(scan: RobotScan) {
        scan.prev = _latest
        _latest = scan
        _snapshots.tryEmit(scan)
        scanListeners.forEach { it.invoke(scan) }
    }

    fun kill() {
        deathListeners.forEach { it.invoke() }
    }

    fun revive(scan: RobotScan) {
        _latest = scan
        _snapshots.tryEmit(scan)
        scanListeners.forEach { it.invoke(scan) }
    }

    fun onScan(listener: ScanListener) {
        scanListeners.add(listener)
    }

    fun onDeath(listener: DeathListener) {
        deathListeners.add(listener)
    }
}

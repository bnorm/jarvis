package bnorm.robot

import bnorm.parts.BattleField

typealias ScanListener = suspend (scan: RobotScan) -> Unit
typealias DeathListener = suspend () -> Unit

class Robot constructor(
    val name: String,
    val context: RobotContext,
    val battleField: BattleField
) {
    companion object {
        suspend fun create(
            name: String,
            context: RobotContext,
            battleField: BattleField,
            initial: RobotScan
        ): Robot {
            val robot = Robot(name, context, battleField)
            robot.revive(initial)
            return robot
        }
    }

    private val _history = ArrayDeque<RobotScan>(100_000)
    private val scanListeners = mutableListOf<ScanListener>()
    private val deathListeners = mutableListOf<DeathListener>()

    val latest: RobotScan get() = _history[0]
    val history: Sequence<RobotScan> get() = generateSequence(latest) { it.prev }

    operator fun get(time: Long): RobotScan? {
        val index = _history.binarySearchBy(time) { it.time }
        return if (index >= 0) _history[index] else null
    }

    suspend fun scan(scan: RobotScan) {
        scan.prev = _history.firstOrNull()
        _history.addFirst(scan)
        scanListeners.forEach { it.invoke(scan) }
    }

    suspend fun kill() {
        deathListeners.forEach { it.invoke() }
    }

    suspend fun revive(scan: RobotScan) {
        _history.clear()
        _history.addFirst(scan)
        scanListeners.forEach { it.invoke(scan) }
    }

    fun onScan(listener: ScanListener) {
        scanListeners.add(listener)
    }

    fun onDeath(listener: DeathListener) {
        deathListeners.add(listener)
    }
}

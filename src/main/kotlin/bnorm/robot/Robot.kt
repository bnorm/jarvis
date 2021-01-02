package bnorm.robot

import bnorm.parts.BattleField

class Robot(
    val name: String,
    val context: RobotContext,
    val battleField: BattleField,
    initial: RobotScan
) {
    private val _history = ArrayDeque<RobotScan>(100_000)
    private val scanListeners = mutableListOf<ScanListener>()
    private val deathListeners = mutableListOf<DeathListener>()

    init {
        revive(initial)
    }

    val latest: RobotScan get() = _history[0]
    val history: Sequence<RobotScan> get() = generateSequence(latest) { it.prev }

    operator fun get(time: Long): RobotScan? {
        val index = _history.binarySearchBy(time) { it.time }
        return if (index >= 0) _history[index] else null
    }

    fun scan(scan: RobotScan) {
        scan.prev = _history.firstOrNull()
        _history.addFirst(scan)
        scanListeners.forEach { it.notifyScan(scan) }
    }

    fun kill() {
        deathListeners.forEach { it.notifyDeath() }
    }

    fun revive(scan: RobotScan) {
        _history.clear()
        _history.addFirst(scan)
        scanListeners.forEach { it.notifyScan(scan) }
    }

    fun onScan(listener: ScanListener) {
        scanListeners.add(listener)
    }

    fun onDeath(listener: DeathListener) {
        deathListeners.add(listener)
    }

    fun interface ScanListener {
        fun notifyScan(scan: RobotScan)
    }

    fun interface DeathListener {
        fun notifyDeath()
    }
}

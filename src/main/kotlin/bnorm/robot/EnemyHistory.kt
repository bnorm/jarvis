package bnorm.robot

class EnemyHistory(
    scan: RobotScan
) : RobotHistory {
    private val history = ArrayDeque<RobotScan>(1_000_000)

    init {
        add(scan)
    }

    override val latest: RobotScan get() = history.first()

    override fun add(scan: RobotScan) {
        history.addFirst(scan)
    }

    override fun iterator(): Iterator<RobotScan> = history.iterator()
}

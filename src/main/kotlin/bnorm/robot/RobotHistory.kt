package bnorm.robot

interface RobotHistory : Sequence<RobotScan> {
    val latest: RobotScan

    fun add(scan: RobotScan)
}

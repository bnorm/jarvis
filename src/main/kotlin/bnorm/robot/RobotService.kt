package bnorm.robot

import bnorm.r2

class RobotService {
    private val _alive = mutableMapOf<String, Robot>()
    val alive: Collection<Robot> = _alive.values

    operator fun get(name: String): Robot? = _alive[name]
    fun closest(x: Double, y: Double): Robot? = _alive.values.minByOrNull { it.history.latest.location.r2(x, y) }

    fun onScan(name: String, scan: RobotScan) {
        when (val existing = _alive[name]) {
            null -> _alive[name] = Robot(name, EnemyHistory(scan))
            else -> {
//                println("$name lag of ${scan.time - existing.history.latest.time}")
                // TODO scan lag is > 1, interpolate?
                existing.history.add(scan)
            }
        }
    }

    fun onDeath(name: String) {
        _alive.remove(name)
    }

    fun onRoundEnd() {

    }
}

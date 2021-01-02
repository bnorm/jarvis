package bnorm.robot

import bnorm.parts.BattleField
import bnorm.r2

class RobotService(
    private val block: RobotService.(Robot) -> Unit
) {
    private val _alive = mutableMapOf<String, Robot>()
    val alive: Collection<Robot> = _alive.values

    private val dead = mutableMapOf<String, Robot>()

    private var _self: Robot? = null
    val self: Robot get() = _self!!

    operator fun get(name: String): Robot? = _alive[name]
    fun closest(x: Double, y: Double): Robot? = _alive.values.minByOrNull { it.latest.location.r2(x, y) }

    fun <C : Any, V : Any, F : RobotContext.Feature<C, V>> Robot.install(
        feature: F,
        block: C.() -> Unit
    ) {
        val robot = this
        val value = with(feature) { install(robot, block) }
        robot.context[feature] = value
    }


    fun onScan(name: String, scan: RobotScan, battleField: BattleField) {
        when (val existing = _alive[name]) {
            null -> _alive[name] = dead[name]?.apply { revive(scan) } ?: run {
                Robot(name, RobotContext(), battleField, scan).also { block(it) }
            }
            else -> existing.scan(scan) // TODO scan lag is > 1, interpolate?
        }
    }

    fun onStatus(name: String, scan: RobotScan, battleField: BattleField) {
        if (_self == null) {
            _self = Robot(name, RobotContext(), battleField, scan)
        } else {
            val self = _self!!
            if (self.latest.time > scan.time) {
                self.revive(scan)
            } else {
                self.scan(scan)
            }
        }
    }

    fun onKill(name: String) {
        val existing = _alive.remove(name)
        if (existing != null) {
            existing.kill()
            dead[name] = existing
        }
    }

    fun onRoundEnd() {
        for ((name, existing) in _alive) {
            existing.kill()
            dead[name] = existing
        }
        _alive.clear()
        _self!!.kill()
    }
}

package bnorm.robot

import bnorm.parts.BattleField
import bnorm.r2

class RobotService(
    private val block: suspend RobotService.(Robot) -> Unit
) {
    private val _alive = mutableMapOf<String, Robot>()
    val alive: Collection<Robot> = _alive.values

    private val dead = mutableMapOf<String, Robot>()

    val all: Collection<Robot> get() = (_alive.values + dead.values).distinct()

    private var _self: Robot? = null
    val self: Robot get() = _self!!

    operator fun get(name: String): Robot? = _alive[name]

    suspend fun <C : Any, V : Any, F : RobotContext.Feature<C, V>> Robot.install(
        feature: F,
        block: C.() -> Unit = {}
    ): V {
        val robot = this
        val value = with(feature) { install(robot, block) }
        robot.context[feature] = value
        return value
    }


    suspend fun onScan(name: String, scan: RobotScan, battleField: BattleField) {
        when (val existing = _alive[name]) {
            null -> _alive[name] = dead[name]?.apply { revive(scan) } ?: run {
                Robot.create(name, RobotContext(), battleField, scan).also { block(it) }
            }
            else -> existing.scan(scan) // TODO scan lag is > 1, interpolate?
        }
    }

    suspend fun onStatus(name: String, scan: RobotScan, battleField: BattleField) {
        if (_self == null) {
            _self = Robot.create(name, RobotContext(), battleField, scan)
        } else {
            val self = _self!!
            if (self.latest.time > scan.time) {
                self.revive(scan)
            } else {
                self.scan(scan)
            }
        }
    }

    suspend fun onKill(name: String) {
        val existing = _alive.remove(name)
        if (existing != null) {
            existing.kill()
            dead[name] = existing
        }
    }

    suspend fun onRoundEnd() {
        for ((name, existing) in _alive) {
            existing.kill()
            dead[name] = existing
        }
        _alive.clear()
        _self!!.kill()
    }
}

fun RobotService.closest(): Robot? = alive.minByOrNull {
    val location = self.latest.location
    it.latest.location.r2(location)
}

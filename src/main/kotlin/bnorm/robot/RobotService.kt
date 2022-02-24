package bnorm.robot

import bnorm.parts.BattleField
import bnorm.r2
import bnorm.robot.interp.RobotLink
import bnorm.robot.interp.solve

class RobotService(
    private val onSelf: suspend RobotService.(Robot) -> Unit,
    private val onEnemy: suspend RobotService.(Robot) -> Unit,
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
                Robot.create(name, RobotContext(), battleField, scan).also { onEnemy(it) }
            }
            else -> {
                val lag = scan.time - existing.latest.time
                if (lag > 1) {
                    val start = existing.latest
                    val end = scan

                    val chain = sequence {
                        yield(RobotLink(start.location, start.velocity))
                        repeat((lag - 1).toInt()) {
                            yield(RobotLink(start.location, start.velocity))
                        }
                        yield(RobotLink(end.location, end.velocity))
                    }.toList()

                    val solution = chain.solve()

                    for (i in 1..solution.size - 2) {
                        val link = solution[i]
                        existing.scan(RobotScan(link.location, link.velocity, start.energy, start.time + i, true, null, null))
                    }
                }
                existing.scan(scan)
            }
        }
    }

    suspend fun onStatus(name: String, scan: RobotScan, battleField: BattleField) {
        if (_self == null) {
            _self = Robot.create(name, RobotContext(), battleField, scan).also { onSelf(it) }
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

fun RobotService.closest(): Robot? {
    val location = self.latest.location
    return alive.minByOrNull { it.latest.location.r2(location) }
}

package bnorm.parts.radar

import bnorm.robot.Robot

class AdaptiveScan(
    private val radar: Radar,
    private val robots: Collection<Robot>,
    private val target: () -> Robot?,
) : Scan {
    private val targetCache = mutableMapOf<String, TargetScan>()
    private val melee = MeleeScan(radar, robots)
    private val default = InfiniteScan(radar)

    override fun setMove() {
        val selected = when {
            // Need to find all robots in the first 8 ticks of the game
            radar.time < 8 -> default
            else -> {
                val target = target()
                when {
                    target != null -> targetCache.getOrPut(target.name) { TargetScan(radar, target) }
                    robots.size > 1 -> melee
                    else -> default
                }
            }
        }
        selected.setMove()
    }
}

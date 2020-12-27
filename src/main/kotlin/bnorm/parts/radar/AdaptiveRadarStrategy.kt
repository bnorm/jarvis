package bnorm.parts.radar

import bnorm.robot.Robot

class AdaptiveRadarStrategy(
    private val radar: Radar,
    private val robots: Collection<Robot>,
    private val target: () -> Robot?,
) : RadarStrategy {
    private val targetCache = mutableMapOf<String, TargetRadarStrategy>()
    private val melee = MeleeRadarStrategy(radar, robots)
    private val default = InfiniteRadarStrategy(radar)

    override fun setMove() {
        val selected = when {
            // Need to find all robots in the first 8 ticks of the game
            radar.time < 8 -> default
            else -> {
                val target = target()
                when {
                    target != null -> targetCache.getOrPut(target.name) { TargetRadarStrategy(radar, target) }
                    robots.size > 1 -> melee
                    else -> default
                }
            }
        }
        selected.setMove()
    }
}

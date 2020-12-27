package bnorm.parts.gun

import bnorm.Vector
import bnorm.robot.Robot

class VirtualTargetingStrategy(
    private val gun: Gun,
    strategies: List<TargetingStrategy>
) : TargetingStrategy {
    init {
        require(strategies.isNotEmpty())
    }

    private class VirtualGun(
        val strategy: TargetingStrategy
    ): Comparable<VirtualGun> {
        var fired: Long = 0
        var hit: Long = 0

        companion object {
            private val comparator = compareBy<VirtualGun> { it.hit.toDouble() / it.fired.toDouble() }
        }

        override fun compareTo(other: VirtualGun): Int = comparator.compare(this, other)
    }

    private val strategies = strategies.map { VirtualGun(it) }

    override fun predict(robot: Robot, bulletPower: Double): Vector {
        if (gun.heat == 0.0) {

        }

        val selected = strategies.minOrNull()!!.strategy
        return selected.predict(robot, bulletPower)
    }
}

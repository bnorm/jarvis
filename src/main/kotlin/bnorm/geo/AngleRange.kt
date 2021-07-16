package bnorm.geo

import robocode.util.Utils
import kotlin.math.PI

class AngleRange(start: Double, end: Double) : ClosedRange<Double> {
    init {
        require(Utils.normalAbsoluteAngle(end - start) < 2 * PI)
    }

    override val start: Double = Utils.normalAbsoluteAngle(start)
    override val endInclusive: Double = relativeToStart(end)

    override fun contains(value: Double): Boolean {
        return relativeToStart(value) <= endInclusive
    }

    private fun relativeToStart(theta: Double): Double {
        return start + Utils.normalAbsoluteAngle(theta - start)
    }
}

package bnorm.geo

import bnorm.normalAbsoluteAngle
import kotlin.math.PI

class AngleRange(start: Double, end: Double) : ClosedRange<Double> {
    override val start: Double = normalAbsoluteAngle(start)
    override val endInclusive: Double = relativeToStart(end)

    init {
        require(this.endInclusive - this.start < 2 * PI)
    }

    override fun contains(value: Double): Boolean {
        return relativeToStart(value) <= endInclusive
    }

    private fun relativeToStart(theta: Double): Double {
        return start + normalAbsoluteAngle(theta - start)
    }
}

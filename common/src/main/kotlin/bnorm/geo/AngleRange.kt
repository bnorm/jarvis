package bnorm.geo

import kotlin.math.PI

class AngleRange(start: Angle, end: Angle) : ClosedRange<Angle> {
    override val start: Angle = start.normalizeAbsolute()
    override val endInclusive: Angle = relativeToStart(end)

    init {
        require(this.endInclusive.radians - this.start.radians < 2 * PI)
    }

    override fun contains(value: Angle): Boolean {
        return relativeToStart(value) <= endInclusive
    }

    private fun relativeToStart(theta: Angle): Angle {
        return start + (theta - start).normalizeAbsolute()
    }
}

operator fun Angle.rangeTo(end: Angle) = AngleRange(this, end)

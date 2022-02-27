package bnorm.parts.radar

import bnorm.geo.Angle

class InfiniteScan(
    private val radar: Radar
) : Scan {
    override fun setMove() {
        radar.setTurn(Angle.POSITIVE_INFINITY)
    }
}

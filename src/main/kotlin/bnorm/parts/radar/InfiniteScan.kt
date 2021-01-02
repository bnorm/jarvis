package bnorm.parts.radar

class InfiniteScan(
    private val radar: Radar
) : Scan {
    override fun setMove() {
        radar.setTurn(Double.POSITIVE_INFINITY)
    }
}

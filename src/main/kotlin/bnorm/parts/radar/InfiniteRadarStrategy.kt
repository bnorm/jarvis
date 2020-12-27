package bnorm.parts.radar

class InfiniteRadarStrategy(
    private val radar: Radar
) : RadarStrategy {
    override fun setMove() {
        radar.setTurn(Double.POSITIVE_INFINITY)
    }
}

package bnorm.debug

import kotlinx.serialization.Serializable

@Serializable
class GuessFactorSnapshot(
    val dimensions: DoubleArray,
    val guessFactor: Double,
)

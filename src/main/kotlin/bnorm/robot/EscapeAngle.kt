package bnorm.robot

import bnorm.parts.gun.virtual.Wave
import bnorm.parts.gun.virtual.WaveContext

data class EscapeAngle(
    val forward: Double,
    val reverse: Double,
) {
    companion object : WaveContext.Feature<EscapeAngle>
}

val Wave.escapeAngle: EscapeAngle get() = context[EscapeAngle]

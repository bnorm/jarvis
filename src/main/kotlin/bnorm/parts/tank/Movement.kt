package bnorm.parts.tank

import bnorm.Vector

interface Movement {
    suspend operator fun invoke(location: Vector.Cartesian, velocity: Vector.Polar): Vector.Polar
}

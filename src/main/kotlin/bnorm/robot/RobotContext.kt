package bnorm.robot

class RobotContext {
    interface Feature<C : Any, V : Any> {
        suspend fun RobotService.install(robot: Robot, block: C.() -> Unit): V
    }

    private val map = mutableMapOf<Feature<*, *>, Any>()

    @Suppress("UNCHECKED_CAST")
    operator fun <F : Any> get(feature: Feature<*, F>): F {
        return map[feature] as F
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <F : Any> set(feature: Feature<*, F>, value: F) {
        map[feature] = value
    }
}

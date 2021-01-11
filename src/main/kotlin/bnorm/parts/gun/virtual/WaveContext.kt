package bnorm.parts.gun.virtual

class WaveContext {
    interface Feature<V : Any>

    private val map = mutableMapOf<Feature<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    operator fun <V : Any> get(feature: Feature<V>): V {
        return map[feature] as V
    }

    @Suppress("UNCHECKED_CAST")
    fun <V : Any> find(feature: Feature<V>): V? {
        return map[feature] as V?
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <V : Any> set(feature: Feature<V>, value: V) {
        map[feature] = value
    }
}

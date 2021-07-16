package bnorm.parts.gun.virtual

class WaveContext {
    interface Key<V : Any>

    private val map = mutableMapOf<Key<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    operator fun <V : Any> get(key: Key<V>): V {
        return map[key] as V
    }

    @Suppress("UNCHECKED_CAST")
    fun <V : Any> find(key: Key<V>): V? {
        return map[key] as V?
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <V : Any> set(key: Key<V>, value: V) {
        map[key] = value
    }
}

package bnorm.plugin


class Context {
    data class Key<T>(val name: String)

    private val map = mutableMapOf<Key<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(key: Key<T>): T {
        return map[key] as T
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> find(key: Key<T>): T? {
        return map[key] as? T
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> set(key: Key<T>, value: T) {
        map[key] = value
    }
}

interface ContextHolder {
    val context: Context
}

operator fun <T : Any> ContextHolder.get(key: Context.Key<T>) = context[key]
operator fun <T : Any> ContextHolder.set(key: Context.Key<T>, value: T) = context.set(key, value)

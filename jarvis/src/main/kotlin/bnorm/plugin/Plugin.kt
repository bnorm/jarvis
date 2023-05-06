@file:Suppress("NOTHING_TO_INLINE")

package bnorm.plugin

interface Plugin<Holder : ContextHolder, Configuration : Any, Value : Any> {
    val key: Context.Key<Value>
    fun install(holder: Holder, configure: Configuration.() -> Unit): Value
}

fun <Holder : ContextHolder, Configuration : Any, Value : Any> Holder.install(
    plugin: Plugin<Holder, Configuration, Value>,
    configure: Configuration.() -> Unit = {}
): Value = when (val installed = context.find(plugin.key)) {
    null -> {
        val value = plugin.install(this, configure)
        context[plugin.key] = value
        value
    }
    plugin -> installed
    else -> throw IllegalStateException("duplicate keys `${plugin.key.name}`")
}

inline operator fun <T : Any> Context.get(plugin: Plugin<*, *, T>) = this[plugin.key]
inline operator fun <Holder : ContextHolder, T : Any> Holder.get(plugin: Plugin<Holder, *, T>) = context[plugin]

inline operator fun <T : Any> Context.set(plugin: Plugin<*, *, T>, value: T) = this.set(plugin.key, value)
inline operator fun <Holder : ContextHolder, T : Any> ContextHolder.set(plugin: Plugin<Holder, *, T>, value: T) =
    context.set(plugin, value)

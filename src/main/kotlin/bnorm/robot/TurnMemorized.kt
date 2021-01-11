package bnorm.robot

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <V : Any> Robot.memorized(factory: () -> V): ReadOnlyProperty<Any?, V> =
    TurnMemorized(this, factory)

private class TurnMemorized<V : Any>(
    private val robot: Robot,
    private val factory: () -> V
) : ReadOnlyProperty<Any?, V> {
    private var time = Long.MIN_VALUE
    private var memorized: V? = null
    override fun getValue(thisRef: Any?, property: KProperty<*>): V {
        if (robot.latest.time != time) {
            memorized = factory()
        }
        return memorized!!
    }
}



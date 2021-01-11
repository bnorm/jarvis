package bnorm.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import java.util.concurrent.ArrayBlockingQueue
import kotlin.coroutines.CoroutineContext

class QueueCoroutineDispatcher : CoroutineDispatcher() {
    companion object {
        private val POISON = object : Any() {}
    }

    private val queue = ArrayBlockingQueue<Any>(100)
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        queue.put(block)
    }

    fun close() {
        queue.clear() // TODO Does this break try-finally?
        queue.offer(POISON)
    }

    fun execute() {
        while (true) {
            val task = queue.take()
            if (task === POISON) {
                close() // close again so other threads see poison-pill
                break
            }
            (task as Runnable).run()
        }
    }
}

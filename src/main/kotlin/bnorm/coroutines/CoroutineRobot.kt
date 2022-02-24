package bnorm.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import robocode.AdvancedRobot
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext

abstract class CoroutineRobot : AdvancedRobot() {
    private val _computation = QueueCoroutineDispatcher(4)
    val Computation: CoroutineDispatcher get() = _computation

    private lateinit var _main: CoroutineDispatcher
    val Main: CoroutineDispatcher get() = _main

    final override fun run() {
        try {
            runBlocking {
                _main = coroutineContext[CoroutineDispatcher]!!
                coroutineRun()
            }
        } finally {
            _computation.shutdown()
        }
    }

    abstract suspend fun coroutineRun()

    private class QueueCoroutineDispatcher(threadCount: Int) : CoroutineDispatcher() {
        companion object {
            private val POISON = Runnable {}
        }
        private val queue = LinkedBlockingQueue<Runnable>()
        private val threads = List(threadCount) {
            thread(name = "Computation $it") {
                try {
                    while (true) {
                        val task = queue.take()
                        if (task === POISON) break
                        task.run()
                    }
                } catch (ignore: InterruptedException) {
                    // ignore
                }
            }
        }

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            queue.put(block)
        }

        fun shutdown() {
            repeat(threads.size) {
                // poison the queue once for each thread
                queue.put(POISON)
            }
        }
    }
}

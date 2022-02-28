package bnorm

import robocode.control.IRobocodeEngine
import robocode.control.RobocodeEngine
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.relativeTo
import kotlin.streams.toList

class RobocodeEnginePool(
    private val robocodeHome: Path
) : AutoCloseable {
    private val workers = ArrayDeque<RobocodeWorker>()

    fun borrow(): IRobocodeEngine {
        val worker = synchronized(workers) { workers.removeFirstOrNull() } ?: worker(robocodeHome)
        return object : IRobocodeEngine by worker.engine {
            override fun close() {
                synchronized(workers) { workers.addFirst(worker) }
            }
        }
    }

    override fun close() {
        generateSequence(workers.removeFirstOrNull()) { workers.removeFirstOrNull() }
            .forEach(RobocodeWorker::close)
    }

    private fun worker(robocodeHome: Path): RobocodeWorker {
        val destination = Files.createTempDirectory("robocode_worker")
        Files.walk(robocodeHome).use { stream ->
            stream.forEach { source ->
                val destination = destination.resolve(source.relativeTo(robocodeHome))
                if (destination.notExists()) {
                    Files.copy(source, destination)
                }
            }
        }

        val urls = Files.walk(destination.resolve("libs"))
            .use { stream -> stream.toList() }
            .filter { it.name.endsWith(".jar") }
            .map { it.toUri().toURL() }
            .toTypedArray()
        val classLoader = URLClassLoader(urls)
        val engineClass = classLoader.loadClass("robocode.control.RobocodeEngine")
        val constructor = engineClass.constructors.single { it.parameterCount == 1 && it.parameters[0].type == File::class.java }

        return RobocodeWorker(destination, constructor.newInstance(destination.toFile()) as RobocodeEngine)
    }

    private class RobocodeWorker(
        private val directory: Path,
        val engine: RobocodeEngine,
    ) : AutoCloseable {
        override fun close() {
            engine.close()
            Files.walk(directory).use { stream ->
                stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete)
            }
        }
    }
}


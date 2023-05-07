package bnorm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.copyTo
import kotlin.io.path.deleteIfExists
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.relativeTo

class BattleExecutor(
    private val robocodeHome: Path,
) : AutoCloseable {
    private val workers = ArrayDeque<Worker>()

    suspend fun run(battle: Battle): Results {
        val worker = synchronized(workers) { workers.removeFirstOrNull() } ?: run {
            val workerHome = Files.createTempDirectory("robocode_worker")
            Files.walk(robocodeHome).use { stream ->
                stream.map { source -> source to workerHome.resolve(source.relativeTo(robocodeHome)) }
                    .filter { (_, destination) -> destination.notExists() }
                    .forEach { (source, destination) -> Files.copy(source, destination) }
            }
            Worker(workerHome)
        }

        try {
            return worker.run(battle)
        } finally {
            synchronized(workers) { workers.addFirst(worker) }
        }
    }

    override fun close() {
        synchronized(workers) {
            for (worker in workers) {
                worker.close()
            }
            workers.clear()
        }
    }

    private class Worker(
        private val workerHome: Path
    ) : AutoCloseable {

        // TODO cache worker directories between runs
        @Suppress("BlockingMethodInNonBlockingContext")
        suspend fun run(battle: Battle): Results = runInterruptible(Dispatchers.IO) {
            val battleFile = workerHome.resolve("battles").resolve("worker.battle")
            val resultsFile = workerHome.resolve("results.txt")

            // Copy bot jar files to worker directory
            for (robot in battle.robots) {
                if (robot.endsWith(".jar")) {
                    Paths.get(ClassLoader.getSystemResource(robot).toURI())
                        .copyTo(workerHome.resolve("robots").resolve(robot), overwrite = true)
                }
            }

            battle.write(battleFile)
            try {
                val process = ProcessBuilder()
                    .directory(workerHome.toFile())
                    .command(
                        listOf(
                            "java",
                            "-Xmx512M",
                            "-Dsun.io.useCanonCaches=false",
                            "-DNOSECURITY=true",
                            "--add-opens=java.base/sun.net.www.protocol.jar=ALL-UNNAMED",
                            "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
                            "--add-opens=java.desktop/javax.swing.text=ALL-UNNAMED",
                            "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
                            "-cp", "libs/robocode.jar",
                            "robocode.Robocode",
                            "-nodisplay",
                            "-battle", "battles/worker.battle",
                            "-results", "results.txt",
                        )
                    )
//                    .inheritIO()
                    .start()
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw IOException("Robocode error: $exitCode")
                }

                resultsFile.readText().parseResults()
            } finally {
                battleFile.deleteIfExists()
                resultsFile.deleteIfExists()
            }
        }

        override fun close() {
            Files.walk(workerHome).use { stream ->
                stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete)
            }
        }
    }
}

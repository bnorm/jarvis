package bnorm.battle

import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.writeText

data class Battle(
    val rounds: Int = 10,
    val robots: List<String>,
    val battleField: BattleField = BattleField()
) {
    data class BattleField(
        val width: Int = 800,
        val height: Int = 600,
    )
}

fun Battle.write(path: Path) {
    val selectedRobots = robots.joinToString(",") {
        if (!it.endsWith(".jar")) it
        else it.replace("_", " ").replace(".jar", "")
    }
    path.writeText(
        text = """
            #Battle Properties
            robocode.battle.numRounds=${rounds}
            robocode.battle.selectedRobots=$selectedRobots
            robocode.battleField.width=${battleField.width}
            robocode.battleField.height=${battleField.height}
            """.trimIndent(),
        options = arrayOf(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
    )
}

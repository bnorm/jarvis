package bnorm

data class Results(
    val robots: List<Robot>
) {
    data class Robot(
        val name: String,
        val totalScore: String,
        val survival: Double,
        val survivalBonus: Double,
        val bulletDamage: Double,
        val bulletBonus: Double,
        val ramDamage: Double,
        val ramBonus: Double,
        val firstCount: Int,
        val secondCount: Int,
        val thirdCount: Int,
    )
}

fun String.parseResults(): Results {
    val lines = lines()

    fun String.parseRobot(): Results.Robot {
        val values = split("\t").iterator()
        return Results.Robot(
            name = values.next().substringAfter(": "),
            totalScore = values.next(),
            survival = values.next().toDouble(),
            survivalBonus = values.next().toDouble(),
            bulletDamage = values.next().toDouble(),
            bulletBonus = values.next().toDouble(),
            ramDamage = values.next().toDouble(),
            ramBonus = values.next().toDouble(),
            firstCount = values.next().toInt(),
            secondCount = values.next().toInt(),
            thirdCount = values.next().toInt(),
        )
    }

    val robots = (2 until lines.size)
        .map { lines[it].trim() }
        .filter { it.isNotBlank() }
        .map { it.parseRobot() }
    return Results(robots)
}

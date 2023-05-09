package bnorm

import kotlin.js.json
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.w3c.fetch.RequestInit

@Serializable
data class BattleData(
    val commits: List<Data>
) {
    @Serializable
    data class Data(
        val commit: Commit,
        val battles: List<Battle>,
    )

    companion object {
        suspend fun get(): BattleData {
            val url = js("require('./battles.json')") as String
            val text = window.fetch(
                input = url,
                init = RequestInit(
                    method = "GET",
                    headers = json(
                        "Content-Type" to "application/json",
                        "Accept" to "application/json",
                        "pragma" to "no-cache"
                    )
                )
            ).await().text().await()
            return Json.decodeFromString(serializer(), text)
        }
    }
}

@Serializable
data class Commit(
    val id: String,
    val abbreviatedId: String,
    val authorName: String,
    val authorEmail: String,
    val committerName: String,
    val committerEmail: String,
    val dateTime: String,
    val shortMessage: String,
    val fullMessage: String,
)

@Serializable
data class Battle(
    val name: String,
    val sessions: Int,
    val groups: List<Group>,
) {
    @Serializable
    data class Group(
        val name: String,
        val results: List<Result>,
    ) {
        @Serializable
        data class Result(
            val name: String,
            val scores: List<Double>,
        )
    }
}

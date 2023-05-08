package bnorm.challenge.wiki

import bnorm.battle.BattleExecutor
import bnorm.challenge.challenge1v1
import bnorm.challenge.toTable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.writeText

class TargetingChallenge {

    private val json = Json { prettyPrint = true }
    private lateinit var executor: BattleExecutor

    @BeforeEach
    fun setup() {
        executor = BattleExecutor(Paths.get(".robocode"))
    }

    @AfterEach
    fun cleanup() {
        executor.close()
    }

    @Test
    fun `Random Movement`() {
        val sessions = System.getenv("BATTLE_SESSIONS")?.toIntOrNull() ?: 10
        val challenge = executor.challenge1v1(
            targetBot = "bnorm.JarvisT*",
            sessions = sessions,
            name = "Targeting Challenge: Random Movement",
            groups = mapOf(
                "Easy" to listOf(
                    "apv.AspidMovement_1.0.jar",
                    "dummy.micro.Sparrow_2.5TC.jar",
                    "kawigi.mini.Fhqwhgads_1.1TC.jar",
                    "emp.Yngwie_1.0.jar",
                    "kawigi.sbf.FloodMini_1.4TC.jar",
                ),
                "Medium" to listOf(
                    "abc.Tron_2.01.jar",
                    "wiki.etc.HTTC_1.0.jar",
                    "wiki.etc.RandomMovementBot_1.0.jar",
                    "davidalves.micro.DuelistMicro_2.0TC.jar",
                    "gh.GrubbmGrb_1.2.4TC.jar",
                ),
                "Hard" to listOf(
                    "pe.SandboxDT_1.91TC.jar",
                    "cx.mini.Cigaret_1.31TC.jar",
                    "kc.Fortune_1.0.jar",
                    "simonton.micro.WeeklongObsession_1.5TC.jar",
                    "jam.micro.RaikoMicro_1.44TC.jar",
                ),
            )
        )
        println(challenge.toTable())
        println(challenge.toWiki("[[Jarvis]]* || [[User:bnorm|bnorm]] || KNN/GF"))

        val directory = Paths.get("build/battles/tc")
        Files.createDirectories(directory)
        directory.resolve("random_movement.json").writeText(json.encodeToString(challenge))

/*
                                                                  Targeting Challenge: Random Movement
─────────────────────────────────────────────────┬───────┬───────────────────────────────────────────────────────────────────────────────────────────────────────────────
                       Bot                       │ Score │ Session 1  Session 2  Session 3  Session 4  Session 5  Session 6  Session 7  Session 8  Session 9  Session 10
────────┬────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
        │                  apv.AspidMovement 1.0 │  95.0 │      96.8       96.9       96.5       90.0       91.4       97.4       93.0       95.0       98.1        95.3
        │              dummy.micro.Sparrow 2.5TC │  98.8 │     100.0       98.4       98.1       99.9       97.4       96.7       99.4       99.9       97.7       100.0
        │            kawigi.mini.Fhqwhgads 1.1TC │  99.0 │      99.9       99.5       99.7       97.0       99.7       98.3       98.7      100.0       99.2        98.3
   Easy │                         emp.Yngwie 1.0 │  97.7 │      97.6       96.3      100.0       97.1       98.0       97.5       95.4      100.0       99.0        95.8
        │             kawigi.sbf.FloodMini 1.4TC │  94.6 │      94.5       83.0       98.4       93.7       96.8       95.1       98.0       94.1       96.0        96.8
        ├────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
        │                           Easy Average │  97.0 │      97.8       94.8       98.5       95.6       96.7       97.0       96.9       97.8       98.0        97.3
────────┼────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
        │                          abc.Tron 2.01 │  91.9 │      92.8       95.9       98.5       91.4       93.0       91.3       91.3       88.2       90.0        87.1
        │                      wiki.etc.HTTC 1.0 │  91.6 │      95.0       94.3       89.1       90.7       91.2       90.1       87.0       97.3       89.5        91.4
        │         wiki.etc.RandomMovementBot 1.0 │  94.5 │      95.7       96.3       88.7       93.4       94.3       98.1       96.3       94.6       89.8        98.0
 Medium │    davidalves.micro.DuelistMicro 2.0TC │  82.2 │      78.5       87.5       84.6       81.6       80.7       81.4       77.9       83.9       81.3        84.9
        │                   gh.GrubbmGrb 1.2.4TC │  87.2 │      87.7       93.0       86.4       87.2       87.4       85.3       80.3       87.5       87.5        90.1
        ├────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
        │                         Medium Average │  89.5 │      90.0       93.4       89.4       88.9       89.3       89.2       86.6       90.3       87.6        90.3
────────┼────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
        │                    pe.SandboxDT 1.91TC │  96.2 │      90.9       94.2       97.0       99.5       99.3       99.4       95.9       96.1       93.8        95.9
        │                 cx.mini.Cigaret 1.31TC │  80.4 │      78.8       85.0       80.9       83.4       67.3       79.2       89.0       80.6       79.3        80.8
        │                         kc.Fortune 1.0 │  83.0 │      76.9       80.1       80.5       91.1       71.4       83.5       87.3       85.2       84.3        89.7
   Hard │ simonton.micro.WeeklongObsession 1.5TC │  83.9 │      83.8       76.9       89.3       85.7       86.1       85.9       84.7       81.1       77.5        87.6
        │            jam.micro.RaikoMicro 1.44TC │  79.7 │      79.2       72.2       90.5       84.0       83.0       81.6       71.1       78.7       78.6        78.1
        ├────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
        │                           Hard Average │  84.6 │      81.9       81.7       87.6       88.8       81.4       85.9       85.6       84.4       82.7        86.4
────────┴────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
                                         Average │  90.4 │      89.9       90.0       91.9       91.1       89.1       90.7       89.7       90.8       89.4        91.3
*/
    }
}

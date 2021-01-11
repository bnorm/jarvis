package bnorm

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import robocode.control.RobocodeEngine
import java.io.File

class TargetingChallenge {
    private lateinit var engine: RobocodeEngine

    @BeforeEach
    fun setup() {
        engine = RobocodeEngine(File(".robocode"))
    }

    @AfterEach
    fun cleanup() {
        engine.close()
    }

    @Test
    fun `Random Movement`() {
        val challenge = engine.challenge1v1(
            targetBot = "bnorm.JarvisT*",
            sessions = 10,
            name = "Targeting Challenge: Random Movement",
            groups = mapOf(
                "Easy" to listOf(
                    "apv.AspidMovement 1.0",
                    "dummy.micro.Sparrow 2.5TC",
                    "kawigi.mini.Fhqwhgads 1.1TC",
                    "emp.Yngwie 1.0",
                    "kawigi.sbf.FloodMini 1.4TC",
                ),
                "Medium" to listOf(
                    "abc.Tron 2.01",
                    "wiki.etc.HTTC 1.0",
                    "wiki.etc.RandomMovementBot 1.0",
                    "davidalves.micro.DuelistMicro 2.0TC",
                    "gh.GrubbmGrb 1.2.4TC",
                ),
                "Hard" to listOf(
                    "pe.SandboxDT 1.91TC",
                    "cx.mini.Cigaret 1.31TC",
                    "kc.Fortune 1.0",
                    "simonton.micro.WeeklongObsession 1.5TC",
                    "jam.micro.RaikoMicro 1.44TC",
                ),
            )
        )
        println(challenge.toTable())

/*
                                                                  Targeting Challenge: Random Movement
─────────────────────────────────────────────────┬───────┬───────────────────────────────────────────────────────────────────────────────────────────────────────────────
                       Bot                       │ Score │ Session 1  Session 2  Session 3  Session 4  Session 5  Session 6  Session 7  Session 8  Session 9  Session 10
────────┬────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
        │                  apv.AspidMovement 1.0 │  92.7 │      92.5       97.0       93.5       86.3       90.2       92.3       92.6       92.6       96.1        93.8
        │              dummy.micro.Sparrow 2.5TC │  97.8 │      97.0       98.6       96.9       96.9       98.9       99.3       96.0       99.4       97.6        97.5
        │            kawigi.mini.Fhqwhgads 1.1TC │  96.7 │      93.4       95.9       98.2       99.3       99.1       97.5       97.4       99.3       95.4        91.2
   Easy │                         emp.Yngwie 1.0 │  98.3 │      97.7       95.1       96.4       99.3       97.8       98.6      100.0       98.8      100.0        99.3
        │             kawigi.sbf.FloodMini 1.4TC │  91.9 │      86.4       91.0       98.6       91.2       96.2       96.1       75.5       95.6       93.6        94.4
        ├────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
        │                           Easy Average │  95.5 │      93.4       95.5       96.7       94.6       96.4       96.7       92.3       97.2       96.5        95.2
────────┼────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
        │                          abc.Tron 2.01 │  88.7 │      81.3       92.4       93.9       89.0       91.4       89.7       88.1       83.4       90.9        87.0
        │                      wiki.etc.HTTC 1.0 │  86.2 │      86.4       91.9       85.9       86.9       77.1       81.5       89.5       93.4       78.1        91.7
        │         wiki.etc.RandomMovementBot 1.0 │  92.1 │      91.7       94.1       99.3       88.5       91.8       90.9       89.8       92.7       89.9        92.0
 Medium │    davidalves.micro.DuelistMicro 2.0TC │  81.1 │      82.9       79.3       78.0       76.7       81.6       86.3       80.5       82.1       82.3        81.3
        │                   gh.GrubbmGrb 1.2.4TC │  75.8 │      71.5       77.7       62.2       77.9       78.6       79.4       79.7       68.1       92.2        70.4
        ├────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
        │                         Medium Average │  84.8 │      82.8       87.1       83.9       83.8       84.1       85.6       85.5       84.0       86.7        84.5
────────┼────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
        │                    pe.SandboxDT 1.91TC │  88.8 │      83.7       93.6       85.5       88.3       90.1       84.7       91.5       91.3       91.8        87.3
        │                 cx.mini.Cigaret 1.31TC │  66.9 │      60.8       64.7       65.4       77.9       62.8       69.3       67.1       68.7       71.0        61.5
        │                         kc.Fortune 1.0 │  81.9 │      86.4       78.7       76.1       85.9       82.1       70.5       85.1       84.1       85.5        84.1
   Hard │ simonton.micro.WeeklongObsession 1.5TC │  81.2 │      85.9       81.2       74.3       84.1       81.0       83.2       84.2       75.7       87.1        74.7
        │            jam.micro.RaikoMicro 1.44TC │  77.1 │      85.9       74.4       82.2       83.3       84.1       81.8       65.7       77.7       68.9        67.2
        ├────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
        │                           Hard Average │  79.2 │      80.5       78.5       76.7       83.9       80.0       77.9       78.7       79.5       80.9        75.0
────────┴────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
                                         Average │  86.5 │      85.6       87.0       85.8       87.4       86.9       86.7       85.5       86.9       88.0        84.9
*/
    }
}

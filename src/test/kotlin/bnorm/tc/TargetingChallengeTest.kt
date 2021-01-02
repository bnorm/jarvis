package bnorm.tc

import bnorm.toTable
import jdk.nashorn.internal.ir.annotations.Ignore
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import robocode.control.RobocodeEngine
import java.io.File

class TargetingChallengeTest {
    private lateinit var engine: RobocodeEngine

    @BeforeEach
    fun setup() {
        engine = RobocodeEngine(File("build/robocode/download"))
    }

    @AfterEach
    fun cleanup() {
        engine.close()
    }

    @Test
    @Ignore
    fun `targeting challenge 2k7`() {
/*
                                                                         Targeting Challenge RM
─────────────────────────────────────────────────┬───────┬───────────────────────────────────────────────────────────────────────────────────────────────────────────────
                       Bot                       │ Score │ Session 1  Session 2  Session 3  Session 4  Session 5  Session 6  Session 7  Session 8  Session 9  Session 10
────────┬────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
        │                  apv.AspidMovement 1.0 │  90.7 │      87.6       87.2       95.3       96.7       96.4       80.8       88.7       95.8       89.5        88.5
        │              dummy.micro.Sparrow 2.5TC │  97.3 │      99.5       95.0       98.5       96.6       95.4       99.3       98.0       97.3       97.0        96.1
        │            kawigi.mini.Fhqwhgads 1.1TC │  94.9 │      88.2       97.3       95.2       96.7       96.9       96.5       94.1       94.7       94.3        95.6
   Easy │                         emp.Yngwie 1.0 │  97.2 │      98.2       96.8       92.2       94.3      100.0       97.5       97.9       99.7       97.5        98.2
        │             kawigi.sbf.FloodMini 1.4TC │  90.3 │      98.6       86.7       91.4       89.5       86.9       88.3       89.2       88.4       94.5        89.2
        ├────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
        │                            Group Total │  90.3 │      98.6       86.7       91.4       89.5       86.9       88.3       89.2       88.4       94.5        89.2
────────┼────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
        │                          abc.Tron 2.01 │  85.5 │      83.2       91.7       91.0       87.4       80.3       83.3       89.3       84.1       83.9        81.1
        │                      wiki.etc.HTTC 1.0 │  85.3 │      76.3       86.7       89.0       90.7       92.1       87.5       84.9       78.5       84.3        83.1
        │         wiki.etc.RandomMovementBot 1.0 │  84.6 │      84.1       84.4       77.3       87.6       86.5       83.1       87.9       87.9       88.5        78.8
 Medium │    davidalves.micro.DuelistMicro 2.0TC │  82.9 │      83.0       77.1       81.6       87.9       90.5       79.3       85.6       78.0       83.3        82.9
        │                   gh.GrubbmGrb 1.2.4TC │  76.0 │      80.7       77.1       69.7       79.5       77.8       78.3       82.2       73.4       71.5        70.2
        ├────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
        │                            Group Total │  90.3 │      98.6       86.7       91.4       89.5       86.9       88.3       89.2       88.4       94.5        89.2
────────┼────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
        │                    pe.SandboxDT 1.91TC │  73.5 │      64.8       78.3       66.5       77.8       67.4       77.3       81.5       75.5       72.0        74.2
        │                 cx.mini.Cigaret 1.31TC │  67.9 │      59.7       72.0       70.5       64.3       67.9       74.9       65.1       69.5       66.4        68.4
        │                         kc.Fortune 1.0 │  75.6 │      75.6       79.9       81.4       79.2       68.5       75.5       71.9       74.3       74.6        75.5
   Hard │ simonton.micro.WeeklongObsession 1.5TC │  78.1 │      77.8       72.1       67.5       82.4       79.5       81.9       84.2       73.9       80.3        81.7
        │            jam.micro.RaikoMicro 1.44TC │  74.0 │      80.3       71.2       68.7       69.5       76.7       72.7       75.0       73.9       70.7        81.7
        ├────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
        │                            Group Total │  90.3 │      98.6       86.7       91.4       89.5       86.9       88.3       89.2       88.4       94.5        89.2
────────┼────────────────────────────────────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────
                                       All Total │  90.3 │      98.6       86.7       91.4       89.5       86.9       88.3       89.2       88.4       94.5        89.2
*/

        val challenge = engine.runTargetChallengeRandomMovement(
            targetBot = "bnorm.Jarvis*",
            sessions = 10
        )
        println(challenge.toTable())
    }
}
package bnorm.parts.gun

import bnorm.parts.RobotPart
import bnorm.parts.contains
import bnorm.robot.Robot
import bnorm.robot.RobotScan

fun RobotPart.generateSequence(
    robot: Robot,
    nextFunction: (prev: RobotScan, curr: RobotScan) -> RobotScan
): Sequence<RobotScan> = sequence {
    val iterator = robot.history.iterator()
    var curr = iterator.next()
    yield(curr)

    var prev =
        if (!iterator.hasNext()) curr
        else {
            val prev = iterator.next()
            if (curr.time - prev.time > 1) curr
            else prev
        }

    while (true) {
        val next = nextFunction(prev, curr)
        if (next.location in battleField) {
            yield(next)
            prev = curr
            curr = next
        } else {
            break
        }
    }

    while (true) yield(curr)
}

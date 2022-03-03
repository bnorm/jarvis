package bnorm.parts.gun

import bnorm.parts.contains
import bnorm.parts.tank.TANK_SIZE
import bnorm.robot.Robot
import bnorm.robot.RobotScan

fun Robot.generateSequence(
    robot: Robot,
    nextFunction: (prev: RobotScan, curr: RobotScan) -> RobotScan
): Sequence<RobotScan> = sequence {
    var curr = robot.latest
    yield(curr)

    var prev = curr.prev?.takeIf { curr.time - it.time == 1L } ?: curr
    while (true) {
        val next = nextFunction(prev, curr)
        if (battleField.contains(next.location, padding = TANK_SIZE / 2)) {
            yield(next)
            prev = curr
            curr = next
        } else {
            break
        }
    }

    while (true) yield(curr)
}

package bnorm.parts.gun

import bnorm.Cartesian
import bnorm.Vector
import bnorm.robot.Robot
import bnorm.robot.RobotScan
import bnorm.robot.RobotService
import bnorm.toCartesian

class VirtualGunService(
    private val gun: Gun,
    private val robotService: RobotService,
    private val strategies: List<TargetingStrategy>,
) : TargetingStrategy {
    class TargetingStrategyVirtualGun(
        val strategy: TargetingStrategy,
        val virtualGun: VirtualGun,
    ) : Comparable<TargetingStrategyVirtualGun> {
        override fun compareTo(other: TargetingStrategyVirtualGun): Int =
            virtualGun.success.compareTo(other.virtualGun.success)
    }

    val robots = mutableMapOf<String, List<TargetingStrategyVirtualGun>>()

    private fun get(name: String) = robots.getOrPut(name) {
        strategies.map { TargetingStrategyVirtualGun(it, VirtualGun()) }
    }

    fun fire(power: Double) {
        val location = Cartesian(gun.x, gun.y)
        val time = gun.time

        for (robot in robotService.alive) {
            for (holder in get(robot.name)) {
                val velocity = holder.strategy.predict(robot, power).toCartesian()
                holder.virtualGun.fire(time, location, velocity, power)
            }
        }
    }

    fun scan(name: String, scan: RobotScan) {
        // TODO each virtual gun targeting the same robot will have the same
        //  radius calculation, can they be combined somehow?
        get(name).forEach { it.virtualGun.scan(scan) }
    }

    fun death(name: String) {
        get(name).forEach { it.virtualGun.death() }
    }

    override fun predict(robot: Robot, bulletPower: Double): Vector {
        val selected = get(robot.name).maxOrNull()!!
        return selected.strategy.predict(robot, bulletPower)
    }
}

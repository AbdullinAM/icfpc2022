package ru.spbstu.icfpc2022

import ru.spbstu.icfpc2022.algo.RectangleCropDummy
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.robovinchi.Robovinchi
import ru.spbstu.icfpc2022.robovinchi.StateCollector
import tornadofx.launch

fun main() {
    val problemId = 8
    StateCollector.pathToProblemImage = "problems/$problemId.png"
    val problems = getProblems()
    val task = Task(problemId, problems.first { it.id == problemId }.initialConfig)
    StateCollector.task = task
    val commands = RectangleCropDummy(task).solve()
    println("Solution size: ${commands.commands.size}")
    launch<Robovinchi>()
}

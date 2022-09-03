package ru.spbstu.icfpc2022

import ru.spbstu.icfpc2022.algo.AutocropDummy
import ru.spbstu.icfpc2022.algo.DummyBlockAverager
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.robovinchi.Robovinchi
import ru.spbstu.icfpc2022.robovinchi.StateCollector
import tornadofx.launch

fun main() {
    val problemId = 2
    StateCollector.pathToProblemImage = "problems/$problemId.png"
    val task = Task(problemId)
    StateCollector.task = task
    val commands = AutocropDummy(task, 27, 4000).solve()
    println("Solution size: ${commands.size}")
    launch<Robovinchi>()
}

package ru.spbstu.icfpc2022

import ru.spbstu.icfpc2022.algo.AutocropDummy
import ru.spbstu.icfpc2022.algo.DummyBlockAverager
import ru.spbstu.icfpc2022.algo.RectangleCropDummy
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.robovinchi.Robovinchi
import ru.spbstu.icfpc2022.robovinchi.StateCollector
import tornadofx.launch

fun main() {
    val problemId = 8
    StateCollector.pathToProblemImage = "problems/$problemId.png"
    val commands = RectangleCropDummy(Task(problemId)).solve()
    println("Solution size: ${commands.size}")
    launch<Robovinchi>()
}

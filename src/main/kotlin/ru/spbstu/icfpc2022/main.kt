package ru.spbstu.icfpc2022

import kotlinx.coroutines.*
import ru.spbstu.icfpc2022.algo.RectangleCropDummy
import ru.spbstu.icfpc2022.algo.StupidFuzzer
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.robovinchi.Robovinchi
import ru.spbstu.icfpc2022.robovinchi.StateCollector
import tornadofx.launch
import kotlin.system.exitProcess

@OptIn(DelicateCoroutinesApi::class)
fun main() {
    val problemId = 13
    StateCollector.pathToProblemImage = "problems/$problemId.png"
    val task = Task(problemId, submissions().bestSubmissions()[problemId]!!.score)
    StateCollector.task = task
    StupidFuzzer(task).solve()
//    val commands = RectangleCropDummy(task).solve()//RectangleCropDummy(task).solve()
//    println("Solution size: ${commands.commands.size}")
//    launch<Robovinchi>()
}

package ru.spbstu.icfpc2022

import ru.spbstu.icfpc2022.algo.RectangleCropDummy
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.robovinchi.Robovinchi
import ru.spbstu.icfpc2022.robovinchi.StateCollector
import tornadofx.launch

fun main() = try {
    val problemId = 1
    val problems = getProblems()
    val submissions = submissions()
    val bestSubmissions = submissions.bestSubmissions()

    val problem = problems.first { it.id == problemId }
    StateCollector.pathToProblemImage = "problems/$problemId.png"
    val im = problem.target
    val bestScore = bestSubmissions[problem.id]?.score
    var task = Task(problem.id, im, problem.initialConfig, bestScore = bestScore)
    StateCollector.task = task
    val commands = RectangleCropDummy(task).solve()
    println("Solution size: ${commands.commands.size}")
    launch<Robovinchi>()
} finally {
    shutdownClient()
}

package ru.spbstu.icfpc2022

import ru.spbstu.icfpc2022.algo.AutocropDummy
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.imageParser.parseImage
import java.net.URL

fun main() {
    val problems = getProblems()
    val submissions = submissions()
    val bestSubmissions = submissions.bestSubmissions()

    val taskId = 1

    val problem = problems.problems.first { it.id == taskId }
    val im = parseImage(URL(problem.target_link))
    val bestScore = bestSubmissions[problem.id]?.score
    var task = Task(problem.id, im, bestScore = bestScore)
    for (colorTolerance in 0..50) {
        for (pixelTolerance in 10..20) {
            for (limit in 500..16000 step 500) {
                val rectangleCropDummy = AutocropDummy(task, colorTolerance, pixelTolerance * 0.05, limit.toLong())
                val solution = rectangleCropDummy.solve()
                if (solution.score < (bestScore ?: Long.MAX_VALUE)) {
                    println("Succeeded with parameters: colorTolerance = $colorTolerance, pixelTolerance = ${pixelTolerance * 0.05}, limit = $limit")
                    submit(problem.id, solution.commands.joinToString("\n"))
                    task = Task(problem.id, im, bestScore = solution.score)
                }
            }
        }
    }
}
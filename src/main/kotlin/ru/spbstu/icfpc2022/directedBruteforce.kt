package ru.spbstu.icfpc2022

import kotlinx.coroutines.*
import ru.spbstu.icfpc2022.algo.AutocropDummy
import ru.spbstu.icfpc2022.algo.CuttingTactic
import ru.spbstu.icfpc2022.algo.RectangleCropDummy
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.imageParser.parseImage
import ru.spbstu.icfpc2022.robovinchi.StateCollector
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

data class Parameters(
    val colorTolerance: Int = 25,
    val pixelTolerance: Int = 15,
    val limit: Int = 7500,
) {
    fun neighbours(): Collection<Parameters> = setOf(
        copy(colorTolerance = colorTolerance + 1),
        copy(colorTolerance = (colorTolerance - 1).coerceAtLeast(0)),
        copy(pixelTolerance = pixelTolerance + 1),
        copy(pixelTolerance = (pixelTolerance - 1).coerceAtLeast(0)),
        copy(limit = limit + 200),
        copy(limit = (limit - 200).coerceAtLeast(0)),
    )
}

fun main(args: Array<String>) {
    try {
        val problems = getProblems()
        val submissions = submissions()
        val bestSubmissions = submissions.bestSubmissions()

        val taskSpec = args.first().split("-")
        val taskIds = when {
            taskSpec.size == 1 -> listOf(taskSpec.single().toInt())
            taskSpec.size == 2 -> (taskSpec[0].toInt() .. taskSpec[1].toInt())
            else -> taskSpec.map { it.toInt() }
        }
        runBlocking {
            withContext(Dispatchers.Default) {
                forEachAsync(taskIds) { taskId ->
                    forEachAsync(CuttingTactic.values().asList()) { cuttingTactic ->
                        val problem = problems.first { it.id == taskId }
                        val im = problem.target
                        val bestScore = bestSubmissions[problem.id]?.score
                        var task = Task(problem.id, im, problem.initialConfig, bestScore = bestScore)
                        StateCollector.turnMeOff = true

                        val visited: MutableSet<Parameters> = Collections.synchronizedSet(mutableSetOf<Parameters>())
                        val que = ConcurrentLinkedQueue<Parameters>()
                        que.add(Parameters())

                        var currentScore = Long.MAX_VALUE
                        var currentWinner = Parameters()
                        while (que.isNotEmpty()) {
                            val next = que.poll()
                            visited.add(next)

                            val top3 = mapAsync(next.neighbours().filter { it !in visited }.asIterable()) {
                                val colorTolerance = it.colorTolerance
                                val pixelTolerance = it.pixelTolerance
                                val limit = it.limit
                                try {
                                    println("Parameters: taskId = $taskId, colorTolerance = $colorTolerance, pixelTolerance = ${pixelTolerance * 0.05}, limit = $limit, cutterTactic = $cuttingTactic")
                                    val rectangleCropDummy = RectangleCropDummy(
                                        task,
                                        colorTolerance,
                                        pixelTolerance * 0.05,
                                        limit.toLong(),
                                        cuttingTactic
                                    )
                                    val solution = rectangleCropDummy.solve()
                                    if (solution.score < task.bestScoreOrMax) {
                                        println("Succeeded with parameters: taskId = $taskId, colorTolerance = $colorTolerance, pixelTolerance = ${pixelTolerance * 0.05}, limit = $limit, cutterTactic = $cuttingTactic")
                                        submit(problem.id, solution.commands.joinToString("\n"))
                                        task = Task(problem.id, im, problem.initialConfig, bestScore = solution.score)
                                    } else {
                                        println("Finished with parameters: taskId = $taskId, colorTolerance = $colorTolerance, pixelTolerance = ${pixelTolerance * 0.05}, limit = $limit, cutterTactic = $cuttingTactic")
                                    }
                                    it to solution.score
                                } catch (e: Throwable) {
                                    System.err.println("Failed with parameters: taskId = $taskId, colorTolerance = $colorTolerance, pixelTolerance = ${pixelTolerance * 0.05}, limit = $limit, cutterTactic = $cuttingTactic")
                                    it to Long.MAX_VALUE
                                }
                            }.sortedBy { it.second }.take(3)

                            for ((neighbour, score) in top3) {
                                if (score < currentScore) {
                                    currentScore = score
                                    currentWinner = neighbour
                                    que.add(neighbour)
                                }
                            }
                        }

                        println("Task#$taskId\ncutterTactic = $cuttingTactic\nlast score: $currentScore\nwith parameters $currentWinner")
                    }
                }
            }
        }


    } finally {
        shutdownClient()
    }
}

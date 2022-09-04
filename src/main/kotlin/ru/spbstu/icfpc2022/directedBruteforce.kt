package ru.spbstu.icfpc2022

import kotlinx.coroutines.*
import ru.spbstu.icfpc2022.algo.*
import ru.spbstu.icfpc2022.algo.tactics.ColoringMethod
import ru.spbstu.icfpc2022.imageParser.parseImage
import ru.spbstu.icfpc2022.robovinchi.StateCollector
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

data class Parameters(
    val colorTolerance: Int = 25,
    val pixelTolerance: Int = 15,
    val limit: Int = 7500
) {
    fun neighbours(): Collection<Parameters> = setOf(
        copy(colorTolerance = (colorTolerance + 2).coerceAtMost(50)),
        copy(colorTolerance = (colorTolerance - 2).coerceAtLeast(0)),
        copy(pixelTolerance = (pixelTolerance + 1).coerceAtMost(20)),
        copy(pixelTolerance = (pixelTolerance - 1).coerceAtLeast(10)),
        copy(limit = (limit + 500).coerceAtMost(16000)),
        copy(limit = (limit - 500).coerceAtLeast(500)),
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
                    val problem = problems.first { it.id == taskId }
                    val im = problem.target
                    val bestScore = bestSubmissions[problem.id]?.score
                    var task = Task(problem.id, im, problem.initialConfig, bestScore = bestScore)
                    StateCollector.turnMeOff = true

                    run {
                        val visited: MutableSet<Parameters> = Collections.synchronizedSet(mutableSetOf<Parameters>())
                        val pending: MutableSet<Parameters> = Collections.synchronizedSet(mutableSetOf<Parameters>())

                        val que = ConcurrentLinkedQueue<Parameters>()
                        que.add(Parameters())

                        var currentScore = Long.MAX_VALUE
                        var currentWinner = Parameters()
                        val cutoff = 1000
                        var stuckCounter = 0
                        while (que.isNotEmpty()) {
                            val next = que.poll()

                            pending.remove(next)
                            visited.add(next) || continue

                            val top3 = mapAsync(next.neighbours().filter { it !in visited && it !in pending }.asIterable()) {
                                val colorTolerance = it.colorTolerance
                                val pixelTolerance = it.pixelTolerance
                                val limit = it.limit
                                try {
                                    val rectangleCropDummy = RectangleCropDummy(
                                        task,
                                        colorTolerance,
                                        pixelTolerance * 0.05,
                                        limit.toLong(),
                                    )
                                    val solution = rectangleCropDummy.solve()
                                    if (solution.score < task.bestScoreOrMax) {
                                        val preamble = "# directedBruteForce, parameters = $it\n"
                                        println(
                                            """Submitting solution, id = ${problem.id}, $it, score = ${solution.score},
                                              |response = ${submit(problem.id, solution.commands.joinToString("\n", prefix = preamble))}
                                              |"""
                                                .trimMargin()
                                        )

                                        task = task.copy(bestScore = solution.score)
                                    }
                                    it to solution.score
                                } catch (e: Throwable) {
                                    System.err.println("Failed with parameters: taskId = $taskId, colorTolerance = $colorTolerance, pixelTolerance = ${pixelTolerance * 0.05}, limit = $limit")
                                    e.printStackTrace()
                                    it to Long.MAX_VALUE
                                }
                            }.groupBy { it.second }.minByOrNull { it.key }?.value.orEmpty()

                            for ((neighbour, score) in top3) {
                                if (score <= currentScore) {
                                    print("|")
                                    if (score < currentScore) {
                                        println("\nNew score: $score, parameters: taskId = $taskId, $neighbour")
                                        currentScore = score
                                        currentWinner = neighbour
                                        stuckCounter = 0
                                    }
                                    if (score == currentScore) {
                                        stuckCounter++
                                        if (stuckCounter > cutoff) {
                                            println("\nStuck limit exceeded, parameters: taskId = $taskId, $neighbour")
                                            continue
                                        }
                                    }

                                    if (neighbour !in pending) {
                                        que.add(neighbour)
                                        pending.add(neighbour)
                                    }
                                }
                            }
                        }

                        println("\nTask#$taskId\nlast score: $currentScore\nwith parameters $currentWinner")
                    }
                }
            }
        }


    } finally {
        shutdownClient()
    }
}

package ru.spbstu.icfpc2022

import kotlinx.coroutines.*
import ru.spbstu.icfpc2022.algo.AutocropDummy
import ru.spbstu.icfpc2022.algo.CuttingTactic
import ru.spbstu.icfpc2022.algo.RectangleCropDummy
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.algo.tactics.DumpSolutions
import ru.spbstu.icfpc2022.algo.tactics.TacticStorage
import ru.spbstu.icfpc2022.robovinchi.StateCollector

suspend fun <T, U> CoroutineScope.mapAsync(collection: Iterable<T>, body:suspend (T) -> U): Collection<U> =
    collection.map { e -> async { body(e) } }.awaitAll()

suspend fun <T, U> CoroutineScope.forEachAsync(collection: Iterable<T>, body:suspend (T) -> U): Unit =
    collection.map { e -> async { body(e) } }.awaitAll().let {  }

fun main(args: Array<String>) {
    try {
        val problems = getProblems()
        val submissions = submissions()
        val bestSubmissions = submissions.bestSubmissions()

        val taskId = args.first().toInt()

        val problem = problems.first { it.id == taskId }
        val im = problem.target
        val bestScore = bestSubmissions[problem.id]?.score
        var task = Task(problem.id, im, problem.initialConfig, bestScore = bestScore)
        StateCollector.turnMeOff = true
        runBlocking {
            withContext(Dispatchers.Default) {
                forEachAsync((0..50 step 2)) { colorTolerance ->
                    forEachAsync((10..20)) { pixelTolerance ->
                        forEachAsync((generateSequence(500) { it + 500 }.take(32).asIterable())) { limit ->
                            forEachAsync(CuttingTactic.values().asList()) { cutterTactic ->
                                try {
                                    println("Parameters: colorTolerance = $colorTolerance, pixelTolerance = ${pixelTolerance * 0.05}, limit = $limit, cutterTactic = $cutterTactic")
                                    val rectangleCropDummy = RectangleCropDummy(
                                        task,
                                        colorTolerance,
                                        pixelTolerance * 0.05,
                                        limit.toLong(),
                                        cutterTactic
                                    )
                                    val solution = rectangleCropDummy.solve()
                                    if (solution.score < task.bestScoreOrMax) {
                                        println("Succeeded with parameters: colorTolerance = $colorTolerance, pixelTolerance = ${pixelTolerance * 0.05}, limit = $limit, cutterTactic = $cutterTactic")
                                        val dumper = DumpSolutions(task, TacticStorage())
                                        dumper(solution)

                                        submit(problem.id, solution.commands.joinToString("\n"))
                                        task = Task(problem.id, im, problem.initialConfig, bestScore = solution.score)
                                    }
                                } catch (e: Throwable) {
                                    System.err.println("Failed with parameters: colorTolerance = $colorTolerance, pixelTolerance = ${pixelTolerance * 0.05}, limit = $limit, cutterTactic = $cutterTactic")
                                }
                            }
                        }
                    }
                }
            }
        }
    } finally {
        shutdownClient()
    }
}

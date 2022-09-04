package ru.spbstu.icfpc2022

import kotlinx.coroutines.*
import ru.spbstu.icfpc2022.algo.*
import ru.spbstu.icfpc2022.algo.tactics.ColoringMethod
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
                forEachAsync((0..50 step 5)) { colorTolerance ->
                    forEachAsync((10..20)) { pixelTolerance ->
                        forEachAsync((generateSequence(1000) { it + 1000 }.take(16).asIterable())) { limit ->
                            run {
                                run {
                                    try {
                                        println(
                                            "Parameters: colorTolerance = $colorTolerance," +
                                                    " pixelTolerance = ${pixelTolerance * 0.05}, " +
                                                    "limit = $limit, "
                                        )
                                        val rectangleCropDummy = RectangleCropDummy(
                                            task,
                                            colorTolerance,
                                            pixelTolerance * 0.05,
                                            limit.toLong()
                                        )
                                        val solution = rectangleCropDummy.solve()
                                        println("${solution.score} | ${if (solution.score >= task.bestScoreOrMax) "worse" else "better"} | ${task.bestScoreOrMax}")
                                        if (solution.score < task.bestScoreOrMax) {
                                            println(
                                                "Succeeded with parameters: colorTolerance = $colorTolerance, " +
                                                        "pixelTolerance = ${pixelTolerance * 0.05}," +
                                                        " limit = $limit,"
                                            )
                                            val dumper = DumpSolutions(task, TacticStorage())
                                            dumper(solution)

                                            val preamble =
                                                "# bruteForce, parameters: colorTolerance = $colorTolerance, " +
                                                        "pixelTolerance = ${pixelTolerance * 0.05}," +
                                                        " limit = $limit\n"
                                            submit(problem.id, solution.commands.joinToString("\n", prefix = preamble))
                                            task =
                                                Task(problem.id, im, problem.initialConfig, bestScore = solution.score)
                                        }
                                    } catch (e: Throwable) {
                                        System.err.println(
                                            "Failed with parameters: colorTolerance = $colorTolerance, " +
                                                    "pixelTolerance = ${pixelTolerance * 0.05}, " +
                                                    "limit = $limit"
                                        )
                                    }
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

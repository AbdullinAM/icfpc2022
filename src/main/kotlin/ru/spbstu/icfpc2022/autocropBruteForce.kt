package ru.spbstu.icfpc2022

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ru.spbstu.icfpc2022.algo.AutocropAverageDummy
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.algo.tactics.DumpSolutions
import ru.spbstu.icfpc2022.algo.tactics.TacticStorage
import ru.spbstu.icfpc2022.robovinchi.StateCollector

fun main(args: Array<String>) = try {
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
                forEachAsync((10..20) step 2) { pixelTolerance ->
                    forEachAsync(10..30 step 5) { defaultWidth ->
                        forEachAsync(1000..10000 step 1000) { limit ->
                            forEachAsync((1..6)) { lowerLimitCoeff1 ->
                                forEachAsync(5..10) { lowerLimitCoeff2 ->
                                    forEachAsync(10..100 step 10) { maxSampleSize ->
                                        try {
                                            val rectangleCropDummy = AutocropAverageDummy(
                                                task,
                                                colorTolerance,
                                                pixelTolerance * 0.05,
                                                defaultWidth,
                                                limit.toLong(),
                                                lowerLimitCoeff1 * 0.05,
                                                lowerLimitCoeff2 * 0.01,
                                                maxSampleSize
                                            )
                                            val solution = rectangleCropDummy.solve()
                                            println(
                                                "${solution.score} |" +
                                                        " ${if (solution.score >= task.bestScoreOrMax) "worse" else "better"} |" +
                                                        " ${task.bestScoreOrMax}"
                                            )
                                            if (solution.score < task.bestScoreOrMax) {
                                                val dumper = DumpSolutions(task, TacticStorage())
                                                dumper(solution)

                                                submit(problem.id, solution.commands.joinToString("\n"))
                                                task =
                                                    Task(
                                                        problem.id,
                                                        im,
                                                        problem.initialConfig,
                                                        bestScore = solution.score
                                                    )
                                            }
                                        } catch (e: Throwable) {
                                            e.printStackTrace()
                                            System.err.println("Failed with parameters")
                                        }
                                    }
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

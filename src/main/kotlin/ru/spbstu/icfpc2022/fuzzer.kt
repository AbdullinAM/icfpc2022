package ru.spbstu.icfpc2022

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ru.spbstu.icfpc2022.algo.StupidFuzzer
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.robovinchi.StateCollector
import kotlin.time.Duration.Companion.minutes

fun main(args: Array<String>) {
    try {
        val taskId = args.first().toInt()
        StateCollector.turnMeOff = true

        val problems = getProblems()
        val submissions = submissions()
        val bestSubmissions = submissions.bestSubmissions()

        val problem = problems.first { it.id == taskId }
        val bestScore = bestSubmissions[problem.id]?.score
        val task = Task(problem.id, problem.target, problem.initialConfig, bestScore = bestScore)

        val fuzzer = StupidFuzzer(
            task,
            iterationTimeout = 30.minutes,
            iterationSize = 2000,
            maxIterations = 2
        )

        runBlocking {
            withContext(Dispatchers.Default) {
                fuzzer.execute()
            }
        }

    } finally {
        shutdownClient()
    }
}

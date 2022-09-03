package ru.spbstu.icfpc2022

import kotlinx.coroutines.*
import ru.spbstu.icfpc2022.algo.AutocropDummy
import ru.spbstu.icfpc2022.algo.CuttingTactic
import ru.spbstu.icfpc2022.algo.RectangleCropDummy
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.imageParser.parseImage
import java.net.URL

suspend fun <T, U> CoroutineScope.mapAsync(collection: Iterable<T>, body:suspend (T) -> U): Collection<U> =
    collection.map { e -> async { body(e) } }.awaitAll()

suspend fun <T, U> CoroutineScope.forEachAsync(collection: Iterable<T>, body:suspend (T) -> U): Unit =
    collection.map { e -> async { body(e) } }.awaitAll().let {  }

fun main() {
    val problems = getProblems()
    val submissions = submissions()
    val bestSubmissions = submissions.bestSubmissions()

    val taskId = 1

    val problem = problems.first { it.id == taskId }
    val im = problem.target
    val bestScore = bestSubmissions[problem.id]?.score
    var task = Task(problem.id, im, problem.initialConfig, bestScore = bestScore)
    runBlocking {
        withContext(Dispatchers.Default) {
            forEachAsync((0..50 step 5)) { colorTolerance ->
                forEachAsync((10..20 step 2)) { pixelTolerance ->
                    forEachAsync((generateSequence(500) { it * 2 }.take(5).asIterable())) { limit ->
                        forEachAsync(CuttingTactic.values().asList()) { cutterTactic ->
                            println("Parameters: colorTolerance = $colorTolerance, pixelTolerance = ${pixelTolerance * 0.05}, limit = $limit, cutterTactic = $cutterTactic")
                            val rectangleCropDummy = RectangleCropDummy(task, colorTolerance, pixelTolerance * 0.05, limit.toLong(), cutterTactic)
                            val solution = rectangleCropDummy.solve()
                            if (solution.score < (bestScore ?: Long.MAX_VALUE)) {
                                println("Succeeded with parameters: colorTolerance = $colorTolerance, pixelTolerance = ${pixelTolerance * 0.05}, limit = $limit, cutterTactic = $cutterTactic")
                                submit(problem.id, solution.commands.joinToString("\n"))
                                task = Task(problem.id, im, problem.initialConfig, bestScore = solution.score)
                            }
                        }
                    }
                }
            }
        }
    }


}

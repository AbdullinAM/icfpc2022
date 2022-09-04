package ru.spbstu.icfpc2022.algo

import kotlinx.coroutines.*
import ru.spbstu.icfpc2022.algo.tactics.*
import ru.spbstu.icfpc2022.canvas.Block
import ru.spbstu.icfpc2022.canvas.BlockId
import ru.spbstu.icfpc2022.canvas.Canvas
import ru.spbstu.icfpc2022.canvas.Point
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.icfpc2022.move.*
import ru.spbstu.icfpc2022.submit
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random
import kotlin.time.Duration

class StupidFuzzer(
    task: Task,
    val iterationTimeout: Duration,
    val iterationSize: Int,
    val maxIterations: Int
) : Solver(task) {

    class BestStateWrapper(
        val score: Long,
        val state: PersistentState
    )

    private val bestState = AtomicReference(
        BestStateWrapper(task.bestScoreOrMax, task.initialState)
    )

    suspend fun execute() {
        for (iteration in 0..maxIterations) {
            val startScore = bestState.get().score
            withTimeout(iterationTimeout) {
                (0..iterationSize).map {
                    launch {
                        withExceptionsIgnored {
                            fuzzerIteration(iteration)
                        }
                    }
                }.joinAll()
            }
            if (bestState.get().score >= startScore) break
        }
    }

    inline fun withExceptionsIgnored(block: () -> Unit): Unit = try {
        block()
    } catch (e: Throwable) {
        System.err.println(e.message)
    }

    private suspend fun fuzzerIteration(iteration: Int) {
        var taskBestScore = if (iteration == 0) task.bestScoreOrMax else bestState.get().score

        val initState = bestState.get().state
        var currentState = initState

        if (iteration == 0) {
            val randomBlock = currentState.canvas.randomBlock() ?: return
            val color = computeBlockAverage(currentState.task.targetImage, randomBlock.shape)
            val move = ColorMove(randomBlock.id, color)
            val coloredInitState = currentState.move(move)
            if (coloredInitState.score < currentState.score) {
                currentState = coloredInitState
                updateBestStateIfBetter(currentState)
            }
        }

        var currentStateScore = currentState.score

        var ind = 0
        while (true) {
            if (++ind % 100 == 0) {
                println("CUR IND = $ind")
            }
            yield()

            if (!shouldContinueIteration(iteration, ind, currentStateScore, taskBestScore)) return

            val newState = if (Random.nextDouble() > 0.3) {
                val randomCutBlock = currentState.canvas.randomBlock() ?: return
                val (cutState, cutBlocks) = randomCut(randomCutBlock, currentState) ?: continue
                val randomColorBlock = cutState.canvas.randomBlock(cutBlocks) ?: continue
                randomColor(randomColorBlock, cutState)
            } else {
                val randomColorBlock = currentState.canvas.randomBlock() ?: return
                randomColor(randomColorBlock, currentState)
            }

            val newStateScore = newState.score
            if (newStateScore < currentStateScore) {
                updateBestStateIfBetter(newState)
                if (newStateScore < taskBestScore) {
                    taskBestScore = newStateScore
                }
                currentStateScore = newStateScore
                currentState = newState
//                val scoreIncrease = newStateScore.toDouble() / task.bestScoreOrMax
//                println("I AM IMPROVED newScore = $newStateScore ind = $ind $scoreIncrease")
            }
        }
    }

    private fun shouldContinueIteration(
        iteration: Int,
        ind: Int,
        currentStateScore: Long,
        taskBestScore: Long
    ): Boolean {
        if (ind > 2000) return false
        if (iteration == 0) {
            if (ind > 30 && currentStateScore.toDouble() / taskBestScore > 1.6) return false
            if (ind > 50 && currentStateScore.toDouble() / taskBestScore > 1.4) return false
            if (ind > 100 && currentStateScore.toDouble() / taskBestScore > 1.2) return false
            if (ind > 250 && currentStateScore.toDouble() / taskBestScore > 1.1) return false
        }
        if (iteration == 1) {
            if (ind > 30 && currentStateScore.toDouble() / taskBestScore > 1.15) return false
            if (ind > 50 && currentStateScore.toDouble() / taskBestScore > 1.07) return false
            if (ind > 100 && currentStateScore.toDouble() / taskBestScore > 1.05) return false
//            if (ind == 250 && coloredStateScore.toDouble() / taskBestScore > 1.0) return false
        }
        //if (ind == 300 && coloredStateScore.toDouble() / task.bestScore!!.toDouble() > 1.05) return currentState
        //if (ind == 400 && coloredStateScore.toDouble() / task.bestScore!!.toDouble() > 1.1) return currentState
        return true
    }

    private fun randomCut(randomBlock: Block, currentState: PersistentState): Pair<PersistentState, Set<BlockId>>? {
        val randomPoint = randomBlock.shape.randomPointInside()
        val randomSnap = task.closestSnap(randomPoint, randomBlock.shape) ?: return null
        val cutMove = if (Random.nextBoolean()) {
            val randomOrientation = Orientation.values().random()
            val randomOffset = when (randomOrientation) {
                Orientation.X -> randomSnap.x
                Orientation.Y -> randomSnap.y
            }
            LineCutMove(randomBlock.id, randomOrientation, randomOffset)
        } else {
            PointCutMove(randomBlock.id, randomSnap)
        }
        val oldStateBlocks = currentState.canvas.blocks.keys
        val newState = currentState.move(cutMove)
        val newStateBlocks = newState.canvas.blocks
        val addedBlocks = newStateBlocks.keys - oldStateBlocks
        return newState to addedBlocks
    }

    private fun randomColor(randomBlock: Block, currentState: PersistentState): PersistentState {
        val coloringMethod = when (Random.nextInt(0, 10)) {
            1, 2 -> ColoringMethod.AVERAGE
            3, 4 -> ColoringMethod.MEDIAN
            5 -> ColoringMethod.MAX
            else -> ColoringMethod.GEOMETRIC_MEDIAN
        }
        val color = computeAverageColor(task.targetImage, randomBlock.shape, coloringMethod)
        val move = ColorMove(randomBlock.id, color)
        return currentState.move(move)
    }

    private fun Canvas.randomBlock(ids: Set<BlockId>? = null): Block? {
        val goodBlocks = blocks.values
            .filter { (ids == null || it.id in ids) && it.shape.width > 1 && it.shape.height > 1 }
        return goodBlocks.randomOrNull()
    }

    private fun Shape.randomPointInside(): Point {
        val x = Random.nextInt(lowerLeftInclusive.x, upperRightExclusive.x)
        val y = Random.nextInt(lowerLeftInclusive.y, upperRightExclusive.y)
        return Point(x, y)
    }

    private fun updateBestStateIfBetter(state: PersistentState) {
        do {
            val currentBest = bestState.get()
            if (state.score >= currentBest.score) return
            val wrapped = BestStateWrapper(state.score, state)
        } while (!bestState.compareAndSet(currentBest, wrapped))

        val dumper = DumpSolutions(task, TacticStorage())
        dumper(state)

        submit(task.problemId, state.dumpSolution())
    }

    override fun solve(): PersistentState {
        error("use execute")
    }

}

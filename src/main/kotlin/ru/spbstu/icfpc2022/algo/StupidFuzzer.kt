package ru.spbstu.icfpc2022.algo

import kotlinx.coroutines.*
import ru.spbstu.icfpc2022.algo.tactics.computeBlockAverage
import ru.spbstu.icfpc2022.algo.tactics.computeBlockMedian
import ru.spbstu.icfpc2022.canvas.BlockId
import ru.spbstu.icfpc2022.canvas.Canvas
import ru.spbstu.icfpc2022.canvas.Point
import ru.spbstu.icfpc2022.move.*
import ru.spbstu.icfpc2022.submit
import kotlin.random.Random
import kotlin.system.exitProcess

class StupidFuzzer(task: Task) : Solver(task) {
    @OptIn(DelicateCoroutinesApi::class)
    fun execute() {
        runBlocking(newFixedThreadPoolContext(16, "hui")) {
            try {
                withTimeout(3_000_000) {
                    val l = (0..1000).map { launch { StupidFuzzer(task).sol() } }
                    l.forEach { it.join() }
                }
            } catch (e: Throwable) { }
        }
    }
    private suspend fun sol(): PersistentState {
        val initState = PersistentState(
            task,
            Canvas.empty(task.targetImage.width, task.targetImage.height)
        )

        val queue = ArrayDeque<PersistentState>()
        queue.add(initState)

        val coloredBlocks = mutableSetOf<BlockId>()

        var currentState = queue.removeFirst()
        var randomBlockId = currentState.canvas.blocks.keys.filter { it !in coloredBlocks }.random()
        var randomBlock = currentState.canvas.blocks[randomBlockId]!!
        var color = computeBlockAverage(currentState.task.targetImage, randomBlock.shape)
        var move = ColorMove(randomBlockId, color)
        currentState = currentState.move(move)
        println("CURRENT STATE SCORE = ${currentState.score}")

        while (true) {
            yield()
            val currentStateScore = currentState.score
            //println("CURRENT STATE SCORE = $currentStateScore")
            while (true) {
                randomBlockId = currentState.canvas.blocks.keys.random()
                randomBlock = currentState.canvas.blocks[randomBlockId]!!

                if (randomBlock.shape.width <= 1 || randomBlock.shape.height <= 1) {
                    continue
                }
                break
            }
            val cutMove = if (Random.nextBoolean()) {
                val randomOrientation = Orientation.values().random()

                val randomOffset = when (randomOrientation) {
                    Orientation.X -> randomBlock.shape.lowerLeft.x + (1 until randomBlock.shape.width).random()
                    Orientation.Y -> randomBlock.shape.lowerLeft.y + (1 until randomBlock.shape.height).random()
                }
                LineCutMove(randomBlockId, randomOrientation, randomOffset)
            } else {
                val randomPoint = Point(
                    randomBlock.shape.lowerLeft.x + (1 until randomBlock.shape.width).random(),
                    randomBlock.shape.lowerLeft.y + (1 until randomBlock.shape.height).random()
                )
                PointCutMove(randomBlockId, randomPoint)
            }
            val newState = currentState.move(cutMove)

            randomBlockId = newState.canvas.blocks.keys.filter { it !in coloredBlocks }.randomOrNull() ?: continue
            randomBlock = newState.canvas.blocks[randomBlockId]!!
            color = computeBlockAverage(newState.task.targetImage, randomBlock.shape)
            move = ColorMove(randomBlockId, color)
            val coloredState = newState.move(move)
            val coloredStateScore = coloredState.score
            //println("NEW STATE SCORE = $coloredStateScore")
            if (coloredStateScore < currentStateScore) {
                println("I AM IMPROVED newScore = ${coloredStateScore}")
                currentState = coloredState
            }
            if (currentStateScore < 31453) {
                println("FOUND BETTER SOLUTION")
                return currentState
            }
        }

        return initState
    }
    override fun solve(): PersistentState {
        TODO()
    }

}
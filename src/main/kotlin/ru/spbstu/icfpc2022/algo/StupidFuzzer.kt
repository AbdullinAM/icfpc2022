package ru.spbstu.icfpc2022.algo

import ru.spbstu.icfpc2022.algo.tactics.computeBlockAverage
import ru.spbstu.icfpc2022.canvas.BlockId
import ru.spbstu.icfpc2022.canvas.Canvas
import ru.spbstu.icfpc2022.canvas.Point
import ru.spbstu.icfpc2022.move.*
import ru.spbstu.icfpc2022.submit

class StupidFuzzer(task: Task) : Solver(task) {

    override fun solve(): PersistentState {
        val initState = PersistentState(
            task,
            Canvas.empty(task.targetImage.width, task.targetImage.height)
        )

        val queue = ArrayDeque<PersistentState>()
        queue.add(initState)

        val coloredBlocks = mutableSetOf<BlockId>()


        while (queue.isNotEmpty()) {
            val currentState = queue.removeFirst()

            val move: Move = when ((1..4).random()) {
                1, 2 -> {
                    // create color move
                    val randomBlockId = currentState.canvas.blocks.keys.filter { it !in coloredBlocks }.randomOrNull() ?: continue
                    val randomBlock = currentState.canvas.blocks[randomBlockId]!!
                    val color = computeBlockAverage(currentState.task.targetImage, randomBlock.shape)
                    coloredBlocks += randomBlockId
                    ColorMove(randomBlockId, color)
                }
                3 -> {
                    // create line cut
                    val randomBlockId = currentState.canvas.blocks.keys.random()
                    val randomBlock = currentState.canvas.blocks[randomBlockId]!!

                    val randomOrientation = Orientation.values().random()

                    if (randomBlock.shape.width <= 1 || randomBlock.shape.height <= 1) {
                        queue.add(currentState)
                        continue
                    }

                    val randomOffset = when (randomOrientation) {
                        Orientation.X -> randomBlock.shape.lowerLeft.x + (1 until randomBlock.shape.width).random()
                        Orientation.Y -> randomBlock.shape.lowerLeft.y + (1 until randomBlock.shape.height).random()
                    }

                    LineCutMove(randomBlockId, randomOrientation, randomOffset)
                }
                4 -> {
                    // create line cut
                    val randomBlockId = currentState.canvas.blocks.keys.random()
                    val randomBlock = currentState.canvas.blocks[randomBlockId]!!

                    if (randomBlock.shape.width <= 1 || randomBlock.shape.height <= 1) {
                        queue.add(currentState)
                        continue
                    }

                    val randomPoint = Point(
                        randomBlock.shape.lowerLeft.x + (1 until randomBlock.shape.width).random(),
                        randomBlock.shape.lowerLeft.y + (1 until randomBlock.shape.height).random()
                    )

                    PointCutMove(randomBlockId, randomPoint)
                }
                else -> error("")
            }

            val newState = currentState.move(move)
            if (newState.score < task.bestScoreOrMax) {
                System.err.println("found smth")
                submit(task.problemId, newState.commands.joinToString("\n"))
            }
            queue.add(currentState)

            if (newState.cost > task.bestScoreOrMax) continue
            queue.add(newState)
        }

        return initState
    }
}
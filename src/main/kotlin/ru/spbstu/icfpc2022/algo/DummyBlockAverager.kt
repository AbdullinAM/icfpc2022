package ru.spbstu.icfpc2022.algo

import ru.spbstu.icfpc2022.canvas.BlockId
import ru.spbstu.icfpc2022.canvas.Canvas
import ru.spbstu.icfpc2022.canvas.Color
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.icfpc2022.imageParser.get
import ru.spbstu.icfpc2022.move.ColorMove
import ru.spbstu.icfpc2022.move.Move
import ru.spbstu.icfpc2022.move.PointCutMove

class DummyBlockAverager(
    task: Task,
    val limit: Long
) : Solver(task) {

    private fun computeBlockAverage(shape: Shape): Color {
        var r = 0L
        var g = 0L
        var b = 0L
        var a = 0L
        var count = 0
        for (x in shape.lowerLeft.x..shape.upperRight.x) {
            for (y in shape.lowerLeft.y..shape.upperRight.y) {
                val pixel = task.targetImage[x, y]
                r += pixel.red()
                g += pixel.green()
                b += pixel.blue()
                a += pixel.alpha()
                count++
            }
        }
        return Color((r / count).toInt(), (g / count).toInt(), (b / count).toInt(), (a / count).toInt())
    }

    override fun solve(): List<Move> {
        var state = PersistentState(
            task,
            Canvas.empty(task.targetImage.width, task.targetImage.height)
        )

        val queue = ArrayDeque<BlockId>()
        queue.addAll(state.canvas.allSimpleBlocks().map { it.id })
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            val currentBlock = state.canvas.blocks[current]!!

            if (currentBlock.shape.size <= limit) continue

            val middlePoint = currentBlock.shape.middle
            val cut = PointCutMove(current, middlePoint)
            val blocksBefore = state.canvas.blocks.keys
            state = state.move(cut)
            val blocksAfter = state.canvas.blocks.keys
            queue.addAll(blocksAfter - blocksBefore)
        }

        for (id in state.canvas.blocks.keys) {
            val block = state.canvas.blocks[id]!!
            val avg = computeBlockAverage(block.shape)

            val colorMove = ColorMove(id, avg)
            state = state.move(colorMove)
        }

        return state.commands
    }
}
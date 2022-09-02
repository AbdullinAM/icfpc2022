package ru.spbstu.icfpc2022.algo

import com.sksamuel.scrimage.nio.PngWriter
import ru.spbstu.icfpc2022.canvas.*
import ru.spbstu.icfpc2022.imageParser.get
import ru.spbstu.icfpc2022.imageParser.getCanvasColor
import ru.spbstu.icfpc2022.imageParser.toImage
import ru.spbstu.icfpc2022.move.ColorMove
import ru.spbstu.icfpc2022.move.Move
import ru.spbstu.icfpc2022.move.PointCutMove
import java.io.File

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

    private fun computeBlockAverage2(shape: Shape): Color {
        val red = IntArray(256)
        val green = IntArray(256)
        val blue = IntArray(256)
        val alpha = IntArray(256)
        for (x in shape.lowerLeft.x..shape.upperRight.x) {
            for (y in shape.lowerLeft.y..shape.upperRight.y) {
                val pixel = task.targetImage[x, y]
                red[pixel.red()]++
                green[pixel.green()]++
                blue[pixel.blue()]++
                alpha[pixel.alpha()]++
            }
        }
        return Color(
            red.withIndex().maxBy { it.value }.index,
            green.withIndex().maxBy { it.value }.index,
            blue.withIndex().maxBy { it.value }.index,
            alpha.withIndex().maxBy { it.value }.index,
        )
    }

    private fun allOneColour(shape: Shape): Boolean {
        val color = task.targetImage[shape.lowerLeft.x, shape.lowerLeft.y].getCanvasColor()
        for (x in shape.lowerLeft.x..shape.upperRight.x) {
            for (y in shape.lowerLeft.y..shape.upperRight.y) {
                val pixel = task.targetImage[x, y]
                if (color != pixel.getCanvasColor()) return false
            }
        }
        return true
    }

    override fun solve(): List<Move> {
        var state = PersistentState(
            task,
            Canvas.empty(task.targetImage.width, task.targetImage.height)
        )

        val maxColor = computeBlockAverage2(Shape(Point(0, 0), Point(state.canvas.width - 1, state.canvas.height - 1)))
        state = state.move(ColorMove(SimpleId(0), maxColor))

        val queue = ArrayDeque<BlockId>()
        queue.addAll(state.canvas.allSimpleBlocks().map { it.id })
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            val currentBlock = state.canvas.blocks[current]!!

            if (currentBlock.shape.size <= limit) continue
            if (allOneColour(currentBlock.shape)) continue

            val middlePoint = currentBlock.shape.middle
            val cut = PointCutMove(current, middlePoint)
            val blocksBefore = state.canvas.blocks.keys
            state = state.move(cut)
            val blocksAfter = state.canvas.blocks.keys
            queue.addAll(blocksAfter - blocksBefore)
        }

        for (id in state.canvas.blocks.keys) {
            val block = state.canvas.blocks[id]!!
            val avg = computeBlockAverage2(block.shape)
            if (avg == maxColor) continue

            val colorMove = ColorMove(id, avg)
            state = state.move(colorMove)
        }

        state.canvas.toImage().forWriter(PngWriter(0)).write(File("solutions/${task.problemId}.png"))
        println(state.score)
        return state.commands
    }
}
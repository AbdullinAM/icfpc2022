package ru.spbstu.icfpc2022.algo.tactics

import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.algo.tactics.AutocropTactic.Companion.approximatelyMatches
import ru.spbstu.icfpc2022.canvas.BlockId
import ru.spbstu.icfpc2022.canvas.Color
import ru.spbstu.icfpc2022.canvas.Point
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.icfpc2022.imageParser.color
import ru.spbstu.icfpc2022.imageParser.get
import ru.spbstu.icfpc2022.imageParser.getCanvasColor
import ru.spbstu.icfpc2022.move.LineCutMove
import ru.spbstu.icfpc2022.move.Orientation
import ru.spbstu.icfpc2022.move.PointCutMove
import kotlin.math.abs
import kotlin.math.sqrt

class ExhaustiveCutter(
    task: Task,
    tacticStorage: TacticStorage,
    val limit: Long,
    val colorTolerance: Int
) : BlockTactic(task, tacticStorage) {
    private fun allOneColour(shape: Shape): Boolean {
        val color = task.targetImage[shape.lowerLeft.x, shape.lowerLeft.y].getCanvasColor()
        for (x in shape.lowerLeft.x..shape.upperRight.x) {
            for (y in shape.lowerLeft.y..shape.upperRight.y) {
                val pixel = task.targetImage[x, y]
                if (!approximatelyMatches(color, pixel.color, colorTolerance)) return false
            }
        }
        return true
    }

    fun diffColors(colors: Collection<Color>): Double {
        if (colors.isEmpty()) return 0.0
        val a = colors.maxOf { it.a }.toDouble() - colors.minOf { it.a }.toDouble()
        val r = colors.maxOf { it.r }.toDouble() - colors.minOf { it.r }.toDouble()
        val g = colors.maxOf { it.g }.toDouble() - colors.minOf { it.g }.toDouble()
        val b = colors.maxOf { it.b }.toDouble() - colors.minOf { it.b }.toDouble()
        return sqrt(a * a + r * r + g * g + b * b)
    }

    override fun invoke(state: PersistentState, blockId: BlockId): PersistentState {
        var state = state
        val queue = ArrayDeque<BlockId>()
        queue.add(blockId)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            val currentBlock = state.canvas.blocks[current]!!

            if (currentBlock.shape.width.toDouble() <= sqrt(limit.toDouble())) continue
            if (currentBlock.shape.height.toDouble() <= sqrt(limit.toDouble())) continue
            if (allOneColour(currentBlock.shape)) continue

            val midPoint = currentBlock.shape.middle
            val halfShape = Shape(
                currentBlock.shape.lowerLeft.midPointWith(midPoint),
                currentBlock.shape.upperRight.midPointWith(midPoint)
            )

            val (xPick) = (halfShape.lowerLeft.x + 1 until halfShape.upperRight.x).map {
                val cut = LineCutMove(current, Orientation.X, it)
                val blocksBefore = state.canvas.blocks.keys
                val newState = state.move(cut, true)
                val blocksAfter = newState.canvas.blocks.keys
                val newBlocks = blocksAfter - blocksBefore

                val colors = newBlocks.map { computeBlockAverage(task.targetImage, newState.canvas.blocks[it]!!.shape) }
                it to diffColors(colors)
            }.groupBy { it.second }.maxBy { it.key }.value.minBy { abs(it.first - midPoint.x) }

            val (yPick) = (halfShape.lowerLeft.y + 1 until halfShape.upperRight.y).map {
                val cut = PointCutMove(current, Point(xPick, it))
                val blocksBefore = state.canvas.blocks.keys
                val newState = state.move(cut, true)
                val blocksAfter = newState.canvas.blocks.keys
                val newBlocks = blocksAfter - blocksBefore

                val colors = newBlocks.map { computeBlockAverage(task.targetImage, newState.canvas.blocks[it]!!.shape) }
                it to diffColors(colors)
            }.groupBy { it.second }.maxBy { it.key }.value.minBy { abs(it.first - midPoint.y) }

            val mid = Point(xPick, yPick)

            val cut = PointCutMove(current, mid)
            val blocksBefore = state.canvas.blocks.keys
            state = state.move(cut)
            val blocksAfter = state.canvas.blocks.keys
            queue.addAll(blocksAfter - blocksBefore)
        }
        return state
    }

}

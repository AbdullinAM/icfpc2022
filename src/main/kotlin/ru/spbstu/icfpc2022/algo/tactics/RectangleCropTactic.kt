package ru.spbstu.icfpc2022.algo.tactics

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.pixels.Pixel
import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.algo.tactics.AutocropTactic.Companion.approx
import ru.spbstu.icfpc2022.canvas.*
import ru.spbstu.icfpc2022.imageParser.get
import ru.spbstu.icfpc2022.imageParser.getCanvasColor
import ru.spbstu.icfpc2022.imageParser.subimage
import ru.spbstu.icfpc2022.move.ColorMove
import ru.spbstu.icfpc2022.move.LineCutMove
import ru.spbstu.icfpc2022.move.Orientation
import ru.spbstu.icfpc2022.move.PointCutMove

class RectangleCropTactic(
    task: Task,
    tacticStorage: TacticStorage,
    val colorTolerance: Int,
    val limit: Long
) : BlockTactic(task, tacticStorage) {
    var leftBlocks = mutableSetOf<BlockId>()

    fun ImmutableImage.pixels(shape: Shape): List<Pixel> {
        val res = mutableListOf<Pixel>()
        for (x in shape.lowerLeft.x..shape.upperRight.x) {
            for (y in shape.lowerLeft.y..shape.upperRight.y) {
                res += this[x, y]
            }
        }
        return res
    }


    override fun invoke(state: PersistentState, blockId: BlockId): PersistentState {
        var state = state
        val queue = ArrayDeque<BlockId>()
        queue.add(blockId)

        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            val currentBlock = state.canvas.blocks[currentId]!!

            var cropPoint = currentBlock.shape.lowerLeft
            var averageColor: Color

            val mutations = mutableListOf(Point(0, 1), Point(1, 0))

            while (true) {
                var succeded = false
                for (mutation in mutations) {
                    val newCrop = cropPoint + mutation
                    if (newCrop.x >= currentBlock.shape.upperRight.x) continue
                    if (newCrop.y >= currentBlock.shape.upperRight.y) continue
                    val newShape = Shape(currentBlock.shape.lowerLeft, newCrop)
                    averageColor = computeBlockAverage(task.targetImage, Shape(currentBlock.shape.lowerLeft, newCrop))

                    if (approx(averageColor, colorTolerance, task.targetImage.pixels(newShape).toTypedArray(), 0.8)) {
                        cropPoint = newCrop
                        succeded = true
                    }
                }
                if (!succeded) break
            }
            val cropShape = Shape(currentBlock.shape.lowerLeft, cropPoint)
            if (cropShape.size < limit) {
                leftBlocks.add(currentId)
                continue
            }

            val cut = when {
                cropPoint == currentBlock.shape.lowerLeft -> {
                    leftBlocks.add(currentId)
                    continue
                }
                cropPoint.x == currentBlock.shape.lowerLeft.x -> LineCutMove(
                    currentId,
                    Orientation.Y,
                    cropPoint.y
                )

                cropPoint.y == currentBlock.shape.lowerLeft.y -> LineCutMove(
                    currentId,
                    Orientation.X,
                    cropPoint.x
                )

                else -> PointCutMove(currentId, cropPoint)
            }
            val oldBlocks = state.canvas.blocks.keys
            state = state.move(cut)
            val newBlocks = state.canvas.blocks.keys

            val createdBlocks = newBlocks - oldBlocks

            val coloringBlock = createdBlocks.first {
                val b = state.canvas.blocks[it]!!
                b.shape.lowerLeft == currentBlock.shape.lowerLeft
            }

            averageColor = computeBlockAverage(task.targetImage, cropShape)
            val colorMove = ColorMove(coloringBlock, averageColor)
            state = state.move(colorMove)

            queue.addAll(createdBlocks - coloringBlock)
        }

        return state
    }

}
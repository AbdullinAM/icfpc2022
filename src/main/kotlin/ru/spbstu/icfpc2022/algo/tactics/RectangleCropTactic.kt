package ru.spbstu.icfpc2022.algo.tactics

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.pixels.Pixel
import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.algo.tactics.AutocropTactic.Companion.approx
import ru.spbstu.icfpc2022.canvas.*
import ru.spbstu.icfpc2022.imageParser.get
import ru.spbstu.icfpc2022.move.ColorMove
import ru.spbstu.icfpc2022.move.LineCutMove
import ru.spbstu.icfpc2022.move.Orientation
import ru.spbstu.icfpc2022.move.PointCutMove

class RectangleCropTactic(
    task: Task,
    tacticStorage: TacticStorage,
    val colorTolerance: Int,
    val pixelTolerance: Double,
    val limit: Long
) : BlockTactic(task, tacticStorage) {
    var leftBlocks = mutableSetOf<BlockId>()

    fun ImmutableImage.pixels(shape: Shape): List<Pixel> {
        val res = mutableListOf<Pixel>()
        for (x in shape.lowerLeftInclusive.x until shape.upperRightExclusive.x) {
            for (y in shape.lowerLeftInclusive.y until shape.upperRightExclusive.y) {
                res += this[x, y]
            }
        }
        return res
    }

    private fun tryLowerLeft(currentBlock: Block): Triple<Point, Point, Shape>? {
        val cropBase = currentBlock.shape.lowerLeftInclusive
        var cropPoint = cropBase
        var averageColor: Color

        val mutations = mutableListOf(Point(0, 1), Point(1, 0))

        while (true) {
            var succeded = false
            for (mutation in mutations) {
                val newCrop = cropPoint + mutation
                if (newCrop.x >= currentBlock.shape.upperRightExclusive.x) continue
                if (newCrop.y >= currentBlock.shape.upperRightExclusive.y) continue
                val newShape = Shape(currentBlock.shape.lowerLeftInclusive, newCrop)
                averageColor = computeBlockAverage(task.targetImage, Shape(currentBlock.shape.lowerLeftInclusive, newCrop))

                if (approx(averageColor, colorTolerance, task.targetImage.pixels(newShape).toTypedArray(), 0.8)) {
                    cropPoint = newCrop
                    succeded = true
                }
            }
            if (!succeded) break
        }
        val cropShape = Shape(cropBase, cropPoint)
        return when {
            cropShape.size < limit -> null
            else -> Triple(cropPoint, cropBase, cropShape)
        }
    }

    private fun tryUpperLeft(currentBlock: Block): Triple<Point, Point, Shape>? {
        val cropBase = currentBlock.shape.let {
            Point(it.lowerLeftInclusive.x, it.upperRightExclusive.y)
        }
        var cropPoint = cropBase - Point(0, 1)
        var averageColor: Color

        val mutations = mutableListOf(Point(0, -1), Point(1, 0))

        while (true) {
            var succeded = false
            for (mutation in mutations) {
                val newCrop = cropPoint + mutation
                if (newCrop.x >= currentBlock.shape.upperRightExclusive.x) continue
                if (newCrop.y <= currentBlock.shape.lowerLeftInclusive.y) continue
                val newShape = Shape(currentBlock.shape.lowerLeftInclusive, newCrop)
                averageColor = computeBlockAverage(task.targetImage, newShape)

                if (approx(averageColor, colorTolerance, task.targetImage.pixels(newShape).toTypedArray(), 0.8)) {
                    cropPoint = newCrop
                    succeded = true
                }
            }
            if (!succeded) break
        }
        val cropShape = Shape(
            lowerLeftInclusive = Point(cropBase.x, cropPoint.y),
            upperRightExclusive = Point(cropPoint.x, cropBase.y)
        )
        return when {
            cropShape.size < limit -> null
            else -> Triple(cropPoint, cropBase, cropShape)
        }
    }

    private fun tryUpperRight(currentBlock: Block): Triple<Point, Point, Shape>? {
        val cropBase = currentBlock.shape.upperRightExclusive
        var cropPoint = cropBase - Point(1, 1)
        var averageColor: Color

        val mutations = mutableListOf(Point(0, -1), Point(-1, 0))

        while (true) {
            var succeded = false
            for (mutation in mutations) {
                val newCrop = cropPoint + mutation
                if (newCrop.x <= currentBlock.shape.lowerLeftInclusive.x) continue
                if (newCrop.y <= currentBlock.shape.lowerLeftInclusive.y) continue
                val newShape = Shape(currentBlock.shape.lowerLeftInclusive, newCrop)
                averageColor = computeBlockAverage(task.targetImage, Shape(currentBlock.shape.lowerLeftInclusive, newCrop))

                if (approx(averageColor, colorTolerance, task.targetImage.pixels(newShape).toTypedArray(), pixelTolerance)) {
                    cropPoint = newCrop
                    succeded = true
                }
            }
            if (!succeded) break
        }
        val cropShape = Shape(cropPoint, cropBase)
        return when {
            cropShape.size < limit -> null
            else -> Triple(cropPoint, cropBase, cropShape)
        }
    }

    private fun tryLowerRight(currentBlock: Block): Triple<Point, Point, Shape>? {
        val cropBase = currentBlock.shape.let {
            Point(it.upperRightExclusive.x, it.lowerLeftInclusive.y)
        }
        var cropPoint = cropBase - Point(1, 0)
        var averageColor: Color

        val mutations = mutableListOf(Point(0, 1), Point(-1, 0))

        while (true) {
            var succeded = false
            for (mutation in mutations) {
                val newCrop = cropPoint + mutation
                if (newCrop.x <= currentBlock.shape.lowerLeftInclusive.x) continue
                if (newCrop.y >= currentBlock.shape.upperRightExclusive.y) continue
                val newShape = Shape(currentBlock.shape.lowerLeftInclusive, newCrop)
                averageColor = computeBlockAverage(task.targetImage, newShape)

                if (approx(averageColor, colorTolerance, task.targetImage.pixels(newShape).toTypedArray(), 0.8)) {
                    cropPoint = newCrop
                    succeded = true
                }
            }
            if (!succeded) break
        }
        val cropShape = Shape(
            Point(cropPoint.x, cropBase.y),
            Point(cropBase.x, cropPoint.y)
        )
        return when {
            cropShape.size < limit -> null
            else -> Triple(cropPoint, cropBase, cropShape)
        }
    }


    override fun invoke(state: PersistentState, blockId: BlockId): PersistentState {
        var state = state
        val queue = ArrayDeque<BlockId>()
        queue.add(blockId)

        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            val currentBlock = state.canvas.blocks[currentId]!!

            val cropOptions = mutableListOf(
                tryLowerLeft(currentBlock),
                tryUpperLeft(currentBlock),
                tryUpperRight(currentBlock),
                tryLowerRight(currentBlock)
            ).filterNotNull()
            if (cropOptions.isEmpty()) {
                leftBlocks.add(currentId)
                continue
            }
            val bestCrop = cropOptions.maxBy { it.third.size }
            val (cropPoint, cropBase, cropShape) = bestCrop

            val cut = when {
                cropPoint == cropBase -> {
                    leftBlocks.add(currentId)
                    continue
                }
                cropPoint.x == cropBase.x -> LineCutMove(
                    currentId,
                    Orientation.Y,
                    cropPoint.y
                )

                cropPoint.y == cropBase.y -> LineCutMove(
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
                b.shape.lowerLeftInclusive == currentBlock.shape.lowerLeftInclusive
            }

            val averageColor = computeBlockAverage(task.targetImage, cropShape)
            val colorMove = ColorMove(coloringBlock, averageColor)
            state = state.move(colorMove)

            queue.addAll(createdBlocks - coloringBlock)
        }

        return state
    }

}
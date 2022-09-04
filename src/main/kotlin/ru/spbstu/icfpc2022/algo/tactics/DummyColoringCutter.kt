package ru.spbstu.icfpc2022.algo.tactics

import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.canvas.BlockId
import ru.spbstu.icfpc2022.canvas.Point
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.icfpc2022.imageParser.color
import ru.spbstu.icfpc2022.imageParser.get
import ru.spbstu.icfpc2022.imageParser.getCanvasColor
import ru.spbstu.icfpc2022.move.PointCutMove

open class DummyColoringCutter(
    task: Task,
    tacticStorage: TacticStorage,
    val limit: Long,
    val colorTolerance: Int,
    val coloringMethod: ColoringMethod
) : BlockTactic(task, tacticStorage) {
    open val useSnaps: Boolean = false

    private fun allOneColour(shape: Shape): Boolean {
        val color = task.targetImage[shape.lowerLeftInclusive.x, shape.lowerLeftInclusive.y].getCanvasColor()
        for (x in shape.lowerLeftInclusive.x until shape.upperRightExclusive.x) {
            for (y in shape.lowerLeftInclusive.y until shape.upperRightExclusive.y) {
                val pixel = task.targetImage[x, y]
                if (!AutocropTactic.approximatelyMatches(color, pixel.color, colorTolerance)) return false
            }
        }
        return true
    }

    override fun invoke(state: PersistentState, blockId: BlockId): PersistentState {
        return colorBlockOptions(state.withIncrementalSimilarity(), blockId).minBy { it.score }
    }

    private fun colorBlockOptions(state: PersistentState, blockId: BlockId): List<PersistentState> {
        val candidates = mutableListOf<PersistentState>()

        val currentBlock = state.canvas.blocks[blockId]!!
        val avg = computeAverageColor(task.targetImage, currentBlock.shape, coloringMethod)

        candidates.add(colorBlock(state, blockId, avg, colorTolerance))

        if (currentBlock.shape.size <= limit) return candidates
        if (allOneColour(currentBlock.shape)) return candidates

        var middlePoint = currentBlock.shape.middle

        if (useSnaps) {
            when (val snap = task.closestCornerSnap(middlePoint, currentBlock.shape)) {
                is Point -> middlePoint = snap
                else -> when (val snap = task.closestSnap(middlePoint, currentBlock.shape)) {
                    null -> {}
                    else -> middlePoint = snap
                }
            }
        }

        val cut = PointCutMove(blockId, middlePoint)
        val blocksBefore = state.canvas.blocks.keys
        var cuttedState = state.move(cut)
        val blocksAfter = cuttedState.canvas.blocks.keys
        val cuttedBlocks = blocksAfter - blocksBefore
        for (cuttedBlock in cuttedBlocks) {
            cuttedState = colorBlockOptions(cuttedState, cuttedBlock).minBy { it.score }
        }
        candidates.add(cuttedState)
        return candidates
    }

}


class DummyColoringCutterWithSnaps(
    task: Task,
    tacticStorage: TacticStorage,
    limit: Long,
    colorTolerance: Int,
    coloringMethod: ColoringMethod
) : DummyColoringCutter(task, tacticStorage, limit, colorTolerance, coloringMethod) {
    override val useSnaps: Boolean
        get() = true
}

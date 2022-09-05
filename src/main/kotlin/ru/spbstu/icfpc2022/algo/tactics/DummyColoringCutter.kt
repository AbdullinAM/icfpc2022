package ru.spbstu.icfpc2022.algo.tactics

import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.canvas.BlockId
import ru.spbstu.icfpc2022.canvas.Point
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.icfpc2022.imageParser.color
import ru.spbstu.icfpc2022.imageParser.get
import ru.spbstu.icfpc2022.imageParser.getCanvasColor
import ru.spbstu.icfpc2022.move.LineCutMove
import ru.spbstu.icfpc2022.move.Orientation
import ru.spbstu.icfpc2022.move.PointCutMove

open class DummyColoringCutter(
    task: Task,
    tacticStorage: TacticStorage,
    val limit: Long,
    val colorTolerance: Int,
//    val coloringMethod: ColoringMethod
) : BlockTactic(task, tacticStorage) {

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

        candidates.add(colorBlockToAverageBest(state, blockId, colorTolerance))

        if (currentBlock.shape.size <= limit) return candidates
        if (allOneColour(currentBlock.shape)) return candidates

        var middlePoint = currentBlock.shape.middle

        val snapPoint = task.closestCornerSnap(middlePoint, currentBlock.shape)
            ?: task.closestSnap(middlePoint, currentBlock.shape)
        for (cut in
            buildSet {
                add(PointCutMove(blockId, middlePoint))
                add(LineCutMove(blockId, Orientation.X, middlePoint.x))
                add(LineCutMove(blockId, Orientation.Y, middlePoint.y))
                if (snapPoint != null) {
                    add(PointCutMove(blockId, snapPoint))
                    add(LineCutMove(blockId, Orientation.X, snapPoint.x))
                    add(LineCutMove(blockId, Orientation.Y, snapPoint.y))
                }
            }
        ) {
            val blocksBefore = state.canvas.blocks.keys
            var cuttedState = state.move(cut, ignoreUI = true)
            val blocksAfter = cuttedState.canvas.blocks.keys
            val cuttedBlocks = blocksAfter - blocksBefore
            for (cuttedBlock in cuttedBlocks) {
                cuttedState = colorBlockOptions(cuttedState, cuttedBlock).minBy { it.score }
            }
            candidates.add(cuttedState)
        }

        return candidates
    }

}

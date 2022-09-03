package ru.spbstu.icfpc2022.algo.tactics

import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.algo.tactics.AutocropTactic.Companion.approximatelyMatches
import ru.spbstu.icfpc2022.canvas.BlockId
import ru.spbstu.icfpc2022.canvas.Point
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.icfpc2022.imageParser.color
import ru.spbstu.icfpc2022.imageParser.get
import ru.spbstu.icfpc2022.imageParser.getCanvasColor
import ru.spbstu.icfpc2022.move.PointCutMove

open class DummyCutter(
    task: Task,
    tacticStorage: TacticStorage,
    val limit: Long,
    val colorTolerance: Int
) : BlockTactic(task, tacticStorage) {
    open val useSnaps: Boolean = false

    private fun allOneColour(shape: Shape): Boolean {
        val color = task.targetImage[shape.lowerLeftInclusive.x, shape.lowerLeftInclusive.y].getCanvasColor()
        for (x in shape.lowerLeftInclusive.x until shape.upperRightExclusive.x) {
            for (y in shape.lowerLeftInclusive.y until shape.upperRightExclusive.y) {
                val pixel = task.targetImage[x, y]
                if (!approximatelyMatches(color, pixel.color, colorTolerance)) return false
            }
        }
        return true
    }

    override fun invoke(state: PersistentState, blockId: BlockId): PersistentState {
        var state = state
        val queue = ArrayDeque<BlockId>()
        queue.add(blockId)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            val currentBlock = state.canvas.blocks[current]!!

            if (currentBlock.shape.size <= limit) continue
            if (allOneColour(currentBlock.shape)) continue

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

            val cut = PointCutMove(current, middlePoint)
            val blocksBefore = state.canvas.blocks.keys
            state = state.move(cut)
            val blocksAfter = state.canvas.blocks.keys
            queue.addAll(blocksAfter - blocksBefore)
        }
        return state
    }

}

class DummyCutterWithSnaps(task: Task, tacticStorage: TacticStorage, limit: Long, colorTolerance: Int) :
    DummyCutter(task, tacticStorage, limit, colorTolerance) {
    override val useSnaps: Boolean
        get() = true
}

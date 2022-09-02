package ru.spbstu.icfpc2022.algo.tactics

import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.canvas.BlockId
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.icfpc2022.imageParser.get
import ru.spbstu.icfpc2022.imageParser.getCanvasColor
import ru.spbstu.icfpc2022.move.PointCutMove

class DummyCutter(
    task: Task,
    tacticStorage: TacticStorage,
    val limit: Long
) : BlockTactic(task, tacticStorage) {
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

//            when (val snap = task.closestSnap(middlePoint, currentBlock.shape)) {
//                null -> {}
//                else -> middlePoint = snap
//            }

            val cut = PointCutMove(current, middlePoint)
            val blocksBefore = state.canvas.blocks.keys
            state = state.move(cut)
            val blocksAfter = state.canvas.blocks.keys
            queue.addAll(blocksAfter - blocksBefore)
        }
        return state
    }

}

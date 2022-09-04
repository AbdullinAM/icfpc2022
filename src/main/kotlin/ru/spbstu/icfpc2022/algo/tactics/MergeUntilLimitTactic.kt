package ru.spbstu.icfpc2022.algo.tactics

import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.canvas.Block
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.icfpc2022.move.MergeMove

class MergeUntilLimitTactic(
    task: Task,
    storage: TacticStorage,
    val limit: Long
) : Tactic(task, storage) {
    fun adjacent(shape1: Shape, shape2: Shape) =
        shape1.isVerticallyAligned(shape2) || shape1.isHorizontallyAligned(shape2)

    override fun invoke(state: PersistentState): PersistentState {
        val blocks = state.canvas.blocks.filterValues { block: Block -> block.shape.size < limit }
        for (block1 in blocks) {
            for (block2 in blocks) if (block1 !== block2) {
                if (adjacent(block1.value.shape, block2.value.shape)) {
                    return invoke(state.move(MergeMove(block1.key, block2.key)))
                }
            }
        }
        return state
    }
}

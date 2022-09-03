package ru.spbstu.icfpc2022.algo.tactics

import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.canvas.Point
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.icfpc2022.move.MergeMove

class MergerTactic(task: Task, storage: TacticStorage) : Tactic(task, storage) {
    fun adjacent(shape1: Shape, shape2: Shape) =
        shape1.isVerticallyAligned(shape2) || shape1.isHorizontallyAligned(shape2)

    override fun invoke(state: PersistentState): PersistentState {
        val calcColors = state.canvas.blocks.values.groupBy { computeBlockMedian(state.task.targetImage, it.shape) }
        for ((_, blocks) in calcColors) {
            for (block1 in blocks) {
                for (block2 in blocks) if (block1 !== block2) {
                    if (adjacent(block1.shape, block2.shape)) {
                        return invoke(state.move(MergeMove(block1.id, block2.id)))
                    }
                }
            }
        }
        return state
    }
}

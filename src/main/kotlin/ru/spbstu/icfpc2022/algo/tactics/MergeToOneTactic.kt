package ru.spbstu.icfpc2022.algo.tactics

import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.icfpc2022.move.MergeMove

class MergeToOneTactic(task: Task, storage: TacticStorage) : Tactic(task, storage) {
    fun adjacent(shape1: Shape, shape2: Shape) =
        shape1.isVerticallyAligned(shape2) || shape1.isHorizontallyAligned(shape2)

    override fun invoke(state: PersistentState): PersistentState {
        val blocks = state.canvas.blocks.values
        val availableMerges = blocks.map { current ->
            current to blocks.filter { it.id != current.id && adjacent(it.shape, current.shape) }
                .maxByOrNull { it.shape.size }
        }
            .filter { it.second != null }.maxByOrNull {
                maxOf(it.first.shape.size, it.second!!.shape.size)
            } ?: return state
        return invoke(state.move(MergeMove(availableMerges.first.id, availableMerges.second!!.id)))
    }
}

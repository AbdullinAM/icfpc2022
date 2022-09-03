package ru.spbstu.icfpc2022.algo.tactics

import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.canvas.BlockId
import ru.spbstu.icfpc2022.move.SwapMove

class SimilaritySwapper(task: Task, storage: TacticStorage) : Tactic(task, storage) {
    override fun invoke(state: PersistentState): PersistentState {
        var current: PersistentState = state.withIncrementalSimilarity()
        val swapped = mutableSetOf<BlockId>()
        while (true) {
            var changed = false

            for (block1 in current.canvas.blocks.keys.shuffled()) {
                val first = current.canvas.blocks[block1]!!
                if (block1 in swapped) continue
                val bestSwap = current.canvas.blocks.keys.filter { block2 ->
                    val second = current.canvas.blocks[block2]!!
                    (block2 !in swapped)
                            && block2 != block1
                            && !(first.shape.width != second.shape.width || first.shape.height != second.shape.height)
                }.mapNotNull { block2 ->
                    val swap = SwapMove(block1, block2)
                    val newState = current.move(swap)
                    if (newState.similarity > current.similarity) null
                    else block2 to newState
                }.minByOrNull { it.second.similarity }
                bestSwap?.let {
                    changed = true
                    swapped += block1
                    swapped += it.first
                    current = it.second
                }
            }

            if (!changed) break
        }

        return current
    }
}
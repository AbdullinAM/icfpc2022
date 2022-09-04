package ru.spbstu.icfpc2022.algo

import ru.spbstu.icfpc2022.algo.tactics.*

class MergeAndColorSolver(
    task: Task,
    val colorTolerance: Int,
    val limit: Long
) : Solver(task) {
    override fun solve(): PersistentState {
        var state = task.initialState

        val storage = TacticStorage()

//        state = SimilaritySwapper(task, storage)(state)
        val merger = MergeUntilLimitTactic(task, storage, limit)
        state = merger(state)

        state = ColorAverageTactic(task, storage, colorTolerance)(state)

        return state
    }
}

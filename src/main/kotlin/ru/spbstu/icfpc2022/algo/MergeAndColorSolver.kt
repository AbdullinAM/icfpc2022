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

        state = ColorBackgroundTactic(task, storage)(state)
        state = OverlayCropTactic(task, storage, colorTolerance)(state)
//        val merger = MergeUntilLimitTactic(task, storage, colorTolerance, limit)
//        state = merger(state)

        state = ColorAverageTactic(task, storage, colorTolerance)(state)

        return state
    }
}

package ru.spbstu.icfpc2022.algo

import ru.spbstu.icfpc2022.algo.tactics.*

class MergeAndColorSolver(
    task: Task,
    val colorTolerance: Int,
    val pixelTolerance: Double,
    val limit: Long,
    val coloringMethod: ColoringMethod
) : Solver(task) {
    override fun solve(): PersistentState {
        var state = task.initialState

        val storage = TacticStorage()

        state = MergeToOneTactic(task, storage)(state)
        state = ColorBackgroundTactic(task, storage)(state)
        state = RandomRectangleCropTactic(task, storage, colorTolerance, pixelTolerance, limit, coloringMethod)(state)

//        state = ColorAverageTactic(task, storage, colorTolerance)(state)

        return state
    }
}

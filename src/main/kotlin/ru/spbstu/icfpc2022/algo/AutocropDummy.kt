package ru.spbstu.icfpc2022.algo

import ru.spbstu.icfpc2022.algo.tactics.*
import ru.spbstu.icfpc2022.canvas.*
import ru.spbstu.icfpc2022.move.*

class AutocropDummy(
    task: Task,
    val colorTolerance: Int = 27,
    val limit: Long = 8000L
) : Solver(task) {
    override fun solve(): PersistentState {
        var state = PersistentState(
            task,
            Canvas.empty(task.targetImage.width, task.targetImage.height)
        )

        val storage = TacticStorage()

        val autocropTactic = AutocropTactic(task, storage, colorTolerance)
        state = autocropTactic(state, SimpleId(0))

        val previousBlocks = state.canvas.blocks.keys
        val dummyCutter = DummyCutter(
            task,
            storage,
            limit,
            colorTolerance
        )
        for (left in autocropTactic.leftBlocks) {
            state = dummyCutter.invoke(state, left)
        }

        val merger = MergerTactic(task, storage)
        state = merger(state)

        val coloringBlocks = state.canvas.blocks.keys - previousBlocks
        for (block in coloringBlocks) {
            state = ColorAverageTactic(task, storage, colorTolerance)(state, block)
        }

        val dumper = DumpSolutions(task, storage)
        dumper(state)

        return state
    }
}

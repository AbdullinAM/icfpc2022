package ru.spbstu.icfpc2022.algo

import ru.spbstu.icfpc2022.algo.tactics.*
import ru.spbstu.icfpc2022.canvas.*
import ru.spbstu.icfpc2022.move.*

class AutocropDummy(
    task: Task,
    val colorTolerance: Int = 27,
    val limit: Long = 8000L
) : Solver(task) {
    override fun solve(): List<Move> {
        var state = PersistentState(
            task,
            Canvas.empty(task.targetImage.width, task.targetImage.height)
        )

        val storage = TacticStorage()
        val cropTactic = AutocropTactic(task, storage, colorTolerance)
        state = cropTactic(state, SimpleId(0))
        val coloringBlocks = listOf(cropTactic.lastUncoloredBlock)

//        val previousBlocks = state.canvas.blocks.keys
//        val dummyCutter = DummyCutter(
//            task,
//            storage,
//            limit,
//            colorTolerance
//        )
//        state = dummyCutter.invoke(state)
//
//        val merger = MergerTactic(task, storage)
//        state = merger(state)
//
//        val coloringBlocks = state.canvas.blocks.keys - previousBlocks
        for (block in coloringBlocks) {
            state = ColorAverageTactic(task, storage, colorTolerance)(state, block)
        }

        val dumper = DumpSolutions(task, storage)
        dumper(state)

        return state.commands
    }
}

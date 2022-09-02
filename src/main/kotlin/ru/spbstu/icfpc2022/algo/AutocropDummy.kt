package ru.spbstu.icfpc2022.algo

import ru.spbstu.icfpc2022.algo.tactics.*
import ru.spbstu.icfpc2022.canvas.*
import ru.spbstu.icfpc2022.move.*

class AutocropDummy(task: Task) : Solver(task) {
    override fun solve(): List<Move> {
        var state = PersistentState(
            task,
            Canvas.empty(task.targetImage.width, task.targetImage.height)
        )

        val storage = TacticStorage()
        state = AutocropTactic(task, storage)(state, SimpleId(0))

        val previousBlocks = state.canvas.blocks.keys
        val dummyCutter = DummyCutter(
            task,
            storage,
            3000
        )
        state = dummyCutter.invoke(state)

        val coloringBlocks = state.canvas.blocks.keys - previousBlocks
        for (block in coloringBlocks) {
            state = ColorAverageTactic(task, storage)(state, block)
        }

        val dumper = DumpSolutions(task, storage)
        dumper(state)

        return state.commands
    }
}

package ru.spbstu.icfpc2022.algo

import ru.spbstu.icfpc2022.algo.tactics.*
import ru.spbstu.icfpc2022.canvas.*
import ru.spbstu.icfpc2022.move.*

class AutocropAverageDummy(
    task: Task,
    val colorTolerance: Int = 27
) : Solver(task) {
    override fun solve(): List<Move> {
        var state = PersistentState(
            task,
            Canvas.empty(task.targetImage.width, task.targetImage.height)
        )

        val storage = TacticStorage()
        val cropTactic = AutocropTactic2(task, storage, colorTolerance)
        state = cropTactic(state, SimpleId(0))
        val coloringBlocks = listOf(cropTactic.lastUncoloredBlock)
        for (block in coloringBlocks) {
            state = ColorAverageTactic(task, storage, colorTolerance)(state, block)
        }

        val dumper = DumpSolutions(task, storage)
        dumper(state)

        return state.commands
    }
}

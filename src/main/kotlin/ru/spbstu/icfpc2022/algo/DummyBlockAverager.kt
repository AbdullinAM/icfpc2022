package ru.spbstu.icfpc2022.algo

import ru.spbstu.icfpc2022.algo.tactics.ColorAverageTactic
import ru.spbstu.icfpc2022.algo.tactics.ColorBackgroundTactic
import ru.spbstu.icfpc2022.algo.tactics.DummyCutter
import ru.spbstu.icfpc2022.algo.tactics.DumpSolutions
import ru.spbstu.icfpc2022.move.Move

class DummyBlockAverager(
    task: Task,
    val limit: Long
) : Solver(task) {

    override fun solve(): List<Move> {
        var state = PersistentState(task)

        val colorBackground = ColorBackgroundTactic(task)
        state = colorBackground(state)
        val cutter = DummyCutter(task, limit)
        state = cutter(state)
        state = ColorAverageTactic(task, backgroundColor = colorBackground.resultingColor)(state)
        val dumper = DumpSolutions(task)
        dumper(state)
        return state.commands
    }
}

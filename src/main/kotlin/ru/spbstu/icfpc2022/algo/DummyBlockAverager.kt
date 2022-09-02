package ru.spbstu.icfpc2022.algo

import ru.spbstu.icfpc2022.algo.tactics.*
import ru.spbstu.icfpc2022.move.Move

fun solve(task: Task, vararg tactics: (Task, TacticStorage) -> Tactic): List<Move> {
    var state = PersistentState(task)
    val storage = TacticStorage()

    val tacticList = tactics.map { it(task, storage) }

    for (tactic in tacticList) state = tactic(state)

    return state.commands
}

class DummyBlockAverager(
    task: Task,
    val limit: Long
) : Solver(task) {

    override fun solve(): List<Move> {
        var state = PersistentState(task)
        val tacticStorage = TacticStorage()

        val colorBackground = ColorBackgroundTactic(task, tacticStorage)
        state = colorBackground(state)
        val cutter = DummyCutter(task, tacticStorage, limit)
        state = cutter(state)
        state = ColorAverageTactic(task, tacticStorage)(state)
        val dumper = DumpSolutions(task, tacticStorage)
        dumper(state)
        return state.commands
    }
}

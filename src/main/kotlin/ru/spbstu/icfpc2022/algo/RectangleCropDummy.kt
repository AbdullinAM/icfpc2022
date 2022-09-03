package ru.spbstu.icfpc2022.algo

import ru.spbstu.icfpc2022.algo.tactics.*
import ru.spbstu.icfpc2022.canvas.Canvas
import ru.spbstu.icfpc2022.canvas.SimpleId
import ru.spbstu.icfpc2022.move.Move

class RectangleCropDummy(
    task: Task,
    val colorTolerance: Int = 27,
    val limit: Long = 5000L
) : Solver(task) {
    override fun solve(): List<Move> {
        var state = PersistentState(
            task,
            Canvas.empty(task.targetImage.width, task.targetImage.height)
        )

        val storage = TacticStorage()

        val autocropTactic = AutocropTactic(task, storage, colorTolerance)
        state = autocropTactic(state, SimpleId(0))

        val rectangleCropTactic = RectangleCropTactic(task, storage, colorTolerance, limit)
        for (left in autocropTactic.leftBlocks) {
            state = rectangleCropTactic(state, left)
        }

        val previousBlocks = state.canvas.blocks.keys
        val dummyCutter = DummyCutter(
            task,
            storage,
            limit,
            colorTolerance
        )
        for (left in rectangleCropTactic.leftBlocks) {
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

        return state.commands
    }
}
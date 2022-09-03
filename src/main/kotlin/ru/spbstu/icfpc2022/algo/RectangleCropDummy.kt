package ru.spbstu.icfpc2022.algo

import ru.spbstu.icfpc2022.algo.tactics.*
import ru.spbstu.icfpc2022.canvas.SimpleId

enum class CuttingTactic { DUMB, DUMBSNAP, EXHAUSTIVE }

class RectangleCropDummy(
    task: Task,
    val colorTolerance: Int = 17,
    val pixelTolerance: Double = 0.95,
    val limit: Long = 5000L,
    val cuttingTactic: CuttingTactic = CuttingTactic.DUMB
) : Solver(task) {
    override fun solve(): PersistentState {
        var state = task.initialState

        val storage = TacticStorage()

        val mergeToOneTactic = MergeToOneTactic(task, storage)
        state = mergeToOneTactic(state)


        val autocropTactic = AutocropTactic(task, storage, colorTolerance, pixelTolerance)
        state = autocropTactic(state, SimpleId(0))

        val rectangleCropTactic = RectangleCropTactic(task, storage, colorTolerance, pixelTolerance, limit)
        for (left in autocropTactic.leftBlocks) {
            state = rectangleCropTactic(state, left)
        }

        val previousBlocks = state.canvas.blocks.keys
        val cutterCreator = when (cuttingTactic) {
            CuttingTactic.EXHAUSTIVE -> ::ExhaustiveCutter
            CuttingTactic.DUMBSNAP -> ::DummyCutterWithSnaps
            else -> ::DummyCutter
        }

        val dummyCutter = cutterCreator(
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

        return state
    }
}

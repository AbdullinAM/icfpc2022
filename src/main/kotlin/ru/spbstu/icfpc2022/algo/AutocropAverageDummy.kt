package ru.spbstu.icfpc2022.algo

import ru.spbstu.icfpc2022.algo.tactics.AutocropTactic2
import ru.spbstu.icfpc2022.algo.tactics.ColorAverageTactic
import ru.spbstu.icfpc2022.algo.tactics.DumpSolutions
import ru.spbstu.icfpc2022.algo.tactics.TacticStorage
import ru.spbstu.icfpc2022.canvas.Canvas
import ru.spbstu.icfpc2022.canvas.SimpleId
import ru.spbstu.icfpc2022.move.Move

class AutocropAverageDummy(
    task: Task,
    val colorTolerance: Int = 27
) : Solver(task) {
    override fun solve(): PersistentState {
        val rootState = PersistentState(
            task,
            Canvas.empty(task.targetImage.width, task.targetImage.height)
        )

        val storage = TacticStorage()
        val cropTactic = AutocropTactic2(task, storage, colorTolerance)
        cropTactic(rootState, SimpleId(0))
        var bestState = rootState
        for ((uncoloredBlock, startState) in cropTactic.finalStates + listOf(SimpleId(0) to rootState)) {
            var state = startState
            val coloringBlocks = listOf(uncoloredBlock)
            for (block in coloringBlocks) {
                state = ColorAverageTactic(task, storage, colorTolerance)(state, block)
            }
            if (state.score < bestState.score) {
                bestState = state
            }
        }

        return bestState
    }
}

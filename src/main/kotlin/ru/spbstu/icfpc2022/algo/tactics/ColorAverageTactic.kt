package ru.spbstu.icfpc2022.algo.tactics

import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.algo.tactics.AutocropTactic.Companion.approximatelyMatches
import ru.spbstu.icfpc2022.canvas.*
import ru.spbstu.icfpc2022.move.ColorMove

class ColorAverageTactic(
    task: Task,
    tacticStorage: TacticStorage,
    val colorTolerance: Int = 17,
    val coloringMethod: ColoringMethod = ColoringMethod.AVERAGE,
    val shouldForceColor: Boolean = false
) : BlockTactic(task, tacticStorage) {
    val backgroundTactic: ColorBackgroundTactic?
        get() = storage.get()
    val backgroundColor: Color?
        get() = backgroundTactic?.resultingColor

    var resultingColor: Color? = null
        private set;

    override operator fun invoke(state: PersistentState, blockId: BlockId): PersistentState {
        val block = state.canvas.blocks[blockId]!!
        val avg = computeAverageColor(task.targetImage, block.shape, coloringMethod)
        if (avg == backgroundColor) return state
        resultingColor = avg

        return if (shouldForceColor) state.move(ColorMove(blockId, avg))
        else colorBlock(state, blockId, avg, colorTolerance)
    }

}

class ColorBackgroundTactic(
    task: Task,
    tacticStorage: TacticStorage,
    coloringMethod: ColoringMethod = ColoringMethod.MAX,
    shouldForceColor: Boolean = false
) : Tactic(task, tacticStorage) {
    val sub = ColorAverageTactic(task, tacticStorage, coloringMethod = coloringMethod, shouldForceColor = shouldForceColor)

    val resultingColor: Color? get() = sub.resultingColor

    override fun invoke(state: PersistentState): PersistentState {
        val bgBlock = state.canvas.blocks.keys.single()
        return sub.invoke(state, bgBlock)
    }
}

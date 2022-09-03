package ru.spbstu.icfpc2022.algo.tactics

import com.sksamuel.scrimage.ImmutableImage
import ru.spbstu.icfpc2022.algo.ColoringMethod
import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.algo.tactics.AutocropTactic.Companion.approximatelyMatches
import ru.spbstu.icfpc2022.canvas.*
import ru.spbstu.icfpc2022.imageParser.get
import ru.spbstu.icfpc2022.move.ColorMove

fun computeAverageColor(image: ImmutableImage, shape: Shape, coloringMethod: ColoringMethod): Color {
    return when (coloringMethod) {
        ColoringMethod.AVERAGE -> computeBlockAverage(image, shape)
        ColoringMethod.MEDIAN -> computeBlockMedian(image, shape)
        ColoringMethod.MAX -> computeBlockMax(image, shape)
    }
}

class ColorAverageTactic(
    task: Task,
    tacticStorage: TacticStorage,
    val colorTolerance: Int = 17,
    val coloringMethod: ColoringMethod = ColoringMethod.AVERAGE
): BlockTactic(task, tacticStorage) {
    val backgroundTactic: ColorBackgroundTactic?
        get() = storage.get()
    val backgroundColor: Color?
        get() = backgroundTactic?.resultingColor

    var resultingColor: Color? = null
        private set;

    override operator fun invoke(state: PersistentState, blockId: BlockId): PersistentState {
        var state = state
        val block = state.canvas.blocks[blockId]!!
        val avg = computeAverageColor(task.targetImage, block.shape, coloringMethod)
        if (avg == backgroundColor) return state
        if (block is SimpleBlock && approximatelyMatches(block.color, avg, colorTolerance)) return state
        resultingColor = avg

        val colorMove = ColorMove(blockId, avg)
        state = state.move(colorMove)
        return state
    }

}

class ColorBackgroundTactic(task: Task, tacticStorage: TacticStorage,
                            coloringMethod: ColoringMethod = ColoringMethod.MAX): Tactic(task, tacticStorage) {
    val sub = ColorAverageTactic(task, tacticStorage, coloringMethod = coloringMethod)

    val resultingColor: Color? get() = sub.resultingColor

    override fun invoke(state: PersistentState): PersistentState {
        val bgBlock = state.canvas.blocks.keys.single()
        return sub.invoke(state, bgBlock)
    }
}

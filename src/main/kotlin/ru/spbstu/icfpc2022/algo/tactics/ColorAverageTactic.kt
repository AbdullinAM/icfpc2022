package ru.spbstu.icfpc2022.algo.tactics

import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.canvas.*
import ru.spbstu.icfpc2022.imageParser.get
import ru.spbstu.icfpc2022.move.ColorMove

class ColorAverageTactic(task: Task, tacticStorage: TacticStorage): BlockTactic(task, tacticStorage) {
    val backgroundTactic: ColorBackgroundTactic?
        get() = storage.get()
    val backgroundColor: Color?
        get() = backgroundTactic?.resultingColor


    var resultingColor: Color? = null
        private set;

    override operator fun invoke(state: PersistentState, blockId: BlockId): PersistentState {
        var state = state
        val block = state.canvas.blocks[blockId]!!
        val avg = computeBlockMedian(state.task.targetImage, block.shape)
        if (avg == backgroundColor) return state
        if (block is SimpleBlock && block.color == avg) return state
        resultingColor = avg

        val colorMove = ColorMove(blockId, avg)
        state = state.move(colorMove)
        return state
    }

}

class ColorBackgroundTactic(task: Task, tacticStorage: TacticStorage): Tactic(task, tacticStorage) {
    val sub = ColorAverageTactic(task, tacticStorage)

    val resultingColor: Color? get() = sub.resultingColor

    override fun invoke(state: PersistentState): PersistentState {
        val bgBlock = SimpleId(0)
        check (bgBlock in state.canvas.blocks)
        return sub.invoke(state, bgBlock)
    }
}

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

    private fun computeBlockAverage(shape: Shape): Color {
        var r = 0L
        var g = 0L
        var b = 0L
        var a = 0L
        var count = 0
        for (x in shape.lowerLeft.x..shape.upperRight.x) {
            for (y in shape.lowerLeft.y..shape.upperRight.y) {
                val pixel = task.targetImage[x, y]
                r += pixel.red()
                g += pixel.green()
                b += pixel.blue()
                a += pixel.alpha()
                count++
            }
        }
        return Color((r / count).toInt(), (g / count).toInt(), (b / count).toInt(), (a / count).toInt())
    }

    private fun computeBlockAverage2(shape: Shape): Color {
        val red = IntArray(256)
        val green = IntArray(256)
        val blue = IntArray(256)
        val alpha = IntArray(256)
        for (x in shape.lowerLeft.x..shape.upperRight.x) {
            for (y in shape.lowerLeft.y..shape.upperRight.y) {
                val pixel = task.targetImage[x, y]
                red[pixel.red()]++
                green[pixel.green()]++
                blue[pixel.blue()]++
                alpha[pixel.alpha()]++
            }
        }
        return Color(
            red.withIndex().maxBy { it.value }.index,
            green.withIndex().maxBy { it.value }.index,
            blue.withIndex().maxBy { it.value }.index,
            alpha.withIndex().maxBy { it.value }.index,
        )
    }

    var resultingColor: Color? = null
        private set;

    override operator fun invoke(state: PersistentState, blockId: BlockId): PersistentState {
        var state = state
        val block = state.canvas.blocks[blockId]!!
        val avg = computeBlockAverage2(block.shape)
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

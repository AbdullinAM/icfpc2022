package ru.spbstu.icfpc2022.algo.tactics

import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.canvas.BlockId
import ru.spbstu.icfpc2022.canvas.Color
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.icfpc2022.imageParser.get
import kotlin.reflect.KClass


abstract class Tactic(val task: Task, val storage: TacticStorage) {
    init {
        @Suppress("LeakingThis")
        storage.add(this)
    }

    abstract operator fun invoke(state: PersistentState): PersistentState

    protected fun computeBlockAverage2(shape: Shape): Color {
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
}

class TacticStorage(
    val data: MutableMap<KClass<out Tactic>, Tactic> = mutableMapOf()
) {
    fun add(tactic: Tactic) {
        data[tactic::class] = tactic
    }

    inline fun <reified T: Tactic> get(): T? = data[T::class] as? T
}

abstract class BlockTactic(task: Task, tacticStorage: TacticStorage): Tactic(task, tacticStorage) {
    abstract operator fun invoke(state: PersistentState, blockId: BlockId): PersistentState

    final override fun invoke(state: PersistentState): PersistentState {
        var state = state
        for (id in state.canvas.blocks.keys) {
            state = invoke(state, id)
        }
        return state
    }
}

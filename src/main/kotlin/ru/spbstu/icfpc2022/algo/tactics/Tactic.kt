package ru.spbstu.icfpc2022.algo.tactics

import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.canvas.BlockId
import ru.spbstu.icfpc2022.move.ColorMove
import kotlin.reflect.KClass

abstract class Tactic(val task: Task) {
    abstract operator fun invoke(state: PersistentState): PersistentState
}

class TacticStorage(
    val data: MutableMap<KClass<out Tactic>, Tactic>
) {
    fun add(tactic: Tactic) {
        data[tactic::class] = tactic
    }

    inline fun <reified T: Tactic> get(): T? = data[T::class] as? T
}

abstract class BlockTactic(task: Task): Tactic(task) {
    abstract operator fun invoke(state: PersistentState, blockId: BlockId): PersistentState

    final override fun invoke(state: PersistentState): PersistentState {
        var state = state
        for (id in state.canvas.blocks.keys) {
            state = invoke(state, id)
        }
        return state
    }
}

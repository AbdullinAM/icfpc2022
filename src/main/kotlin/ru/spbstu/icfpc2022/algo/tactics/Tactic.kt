package ru.spbstu.icfpc2022.algo.tactics

import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.canvas.BlockId
import kotlin.reflect.KClass


abstract class Tactic(val task: Task, val storage: TacticStorage) {
    init {
        @Suppress("LeakingThis")
        storage.add(this)
    }

    abstract operator fun invoke(state: PersistentState): PersistentState
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

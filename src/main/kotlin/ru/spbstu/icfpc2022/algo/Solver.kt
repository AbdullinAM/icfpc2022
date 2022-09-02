package ru.spbstu.icfpc2022.algo

import com.sksamuel.scrimage.ImmutableImage
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import ru.spbstu.icfpc2022.canvas.Canvas
import ru.spbstu.icfpc2022.move.Move


class PersistentState(
    val task: ImmutableImage,
    val canvas: Canvas,
    val commands: PersistentList<Move> = persistentListOf(),
    val cost: Long = 0L
) {
    fun move(move: Move): PersistentState {
        val newCost = cost + canvas.costOf(move)
        return PersistentState(task, canvas.apply(move), commands.add(move), newCost)
    }

    fun dumpSolution(): String = commands.joinToString("\n")
}


abstract class Solver(
    val task: ImmutableImage
) {
    abstract fun solve(): List<Move>
}
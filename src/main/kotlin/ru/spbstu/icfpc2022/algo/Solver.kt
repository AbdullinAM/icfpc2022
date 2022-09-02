package ru.spbstu.icfpc2022.algo

import com.sksamuel.scrimage.ImmutableImage
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import ru.spbstu.icfpc2022.canvas.Canvas
import ru.spbstu.icfpc2022.move.Move


class PersistentState(
    val task: ImmutableImage,
    val canvas: Canvas,
    val commands: PersistentList<Move> = persistentListOf()
) {
    fun move(move: Move) = PersistentState(task, canvas.apply(move), commands.add(move))

    fun dumpSolution(): String = commands.joinToString("\n")
}

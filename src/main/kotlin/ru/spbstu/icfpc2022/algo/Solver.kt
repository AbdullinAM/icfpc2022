package ru.spbstu.icfpc2022.algo

import com.sksamuel.scrimage.ImmutableImage
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import ru.spbstu.icfpc2022.canvas.Canvas
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.icfpc2022.canvas.SimpleBlock
import ru.spbstu.icfpc2022.imageParser.parseImage
import ru.spbstu.icfpc2022.move.Move

data class Task(
    val problemId: Int,
    val targetImage: ImmutableImage
) {
    constructor(problemId: Int): this(problemId, parseImage("problems/$problemId.png"))
}

class PersistentState(
    val task: Task,
    val canvas: Canvas,
    val commands: PersistentList<Move> = persistentListOf()
) {
    fun move(move: Move) = PersistentState(task, canvas.apply(move), commands.add(move))

    fun dumpSolution(): String = commands.joinToString("\n")
}

fun PersistentState(problemId: Int): PersistentState {
    val task = Task(problemId)
    val canvas = Canvas.empty(task.targetImage.width, task.targetImage.height)
    return PersistentState(task, canvas)
}


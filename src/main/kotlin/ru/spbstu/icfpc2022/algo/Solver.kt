package ru.spbstu.icfpc2022.algo

import com.sksamuel.scrimage.ImmutableImage
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import ru.spbstu.icfpc2022.canvas.Canvas
import ru.spbstu.icfpc2022.canvas.Point
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.icfpc2022.canvas.SimpleBlock
import ru.spbstu.icfpc2022.imageParser.parseImage
import ru.spbstu.icfpc2022.imageParser.score
import ru.spbstu.icfpc2022.move.*
import ru.spbstu.icfpc2022.robovinchi.StateCollector
import kotlin.math.round

data class Task(
    val problemId: Int,
    val targetImage: ImmutableImage
) {
    constructor(problemId: Int) : this(problemId, parseImage("problems/$problemId.png"))
}

class PersistentState(
    val task: Task,
    val canvas: Canvas = Canvas.empty(task.targetImage.width, task.targetImage.height),
    val commands: PersistentList<Move> = persistentListOf(),
    val cost: Long = 0L
) {
    val similarity: Double
        get() = score(canvas, task.targetImage)

    val score: Long
        get() = round(similarity * 0.005).toLong() + cost

    fun move(move: Move): PersistentState {
        val newCost = cost + canvas.costOf(move)
        StateCollector.commandToCanvas.add(move to canvas.allSimpleBlocks().toList())
        return PersistentState(task, canvas.apply(move), commands.add(move), newCost)
    }

    fun dumpSolution(): String = commands.joinToString("\n")
}

fun PersistentState(problemId: Int): PersistentState {
    val task = Task(problemId)
    val canvas = Canvas.empty(task.targetImage.width, task.targetImage.height)
    return PersistentState(task, canvas)
}


abstract class Solver(
    val task: Task
) {
    abstract fun solve(): List<Move>
}

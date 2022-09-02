package ru.spbstu.icfpc2022.algo

import com.sksamuel.scrimage.ImmutableImage
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import ru.spbstu.icfpc2022.canvas.Canvas
import ru.spbstu.icfpc2022.canvas.Point
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.icfpc2022.canvas.SimpleBlock
import ru.spbstu.icfpc2022.imageParser.*
import ru.spbstu.icfpc2022.move.Move
import ru.spbstu.icfpc2022.robovinchi.StateCollector
import java.util.TreeMap
import kotlin.math.round

data class Task(
    val problemId: Int,
    val targetImage: ImmutableImage
) {
    constructor(problemId: Int): this(problemId, parseImage("problems/$problemId.png"))

    val snapPoints = TreeMap<Int, TreeMap<Int, Point>>()

    fun addSnap(point: Point) {
        snapPoints.getOrPut(point.x) { TreeMap() }.put(point.y, point)
    }

    fun closestSnap(point: Point, inShape: Shape): Point? {
        val xh = snapPoints.ceilingEntry(point.x)
        val xl = snapPoints.floorEntry(point.x)

        val cand1 = xh?.value?.ceilingEntry(point.y)?.value
        val cand2 = xh?.value?.floorEntry(point.y)?.value
        val cand3 = xl?.value?.ceilingEntry(point.y)?.value
        val cand4 = xl?.value?.floorEntry(point.y)?.value

        return setOfNotNull(cand1, cand2, cand3, cand4)
            .filter { it.isStrictlyInside(inShape.lowerLeft, inShape.upperRight) }.minByOrNull { point.distance(it) }
    }

    init {
        for (pixel in targetImage) {
            val nextRight = targetImage.getOrNull(pixel.x + 1, pixel.y)
            val nextDown = targetImage.getOrNull(pixel.x, pixel.y + 1)
            if (nextRight != null && nextRight.color != pixel.color) addSnap(pixel.point)
            if (nextDown != null && nextDown.color != pixel.color) addSnap(pixel.point)
        }
    }
}

class PersistentState(
    val task: Task,
    val canvas: Canvas = Canvas.empty(task.targetImage.width, task.targetImage.height),
    val commands: PersistentList<Move> = persistentListOf(),
    val cost: Long = 0L
) {
    val similarity: Double get() = score(canvas, task.targetImage)
    val score = round(similarity * 0.005).toLong() + cost

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

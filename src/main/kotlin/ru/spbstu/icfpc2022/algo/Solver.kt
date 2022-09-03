package ru.spbstu.icfpc2022.algo

import com.sksamuel.scrimage.ImmutableImage
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentMap
import ru.spbstu.icfpc2022.InitialConfig
import ru.spbstu.icfpc2022.algo.tactics.AutocropTactic
import ru.spbstu.icfpc2022.canvas.Canvas
import ru.spbstu.icfpc2022.canvas.Color
import ru.spbstu.icfpc2022.canvas.Point
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.icfpc2022.canvas.SimpleId
import ru.spbstu.icfpc2022.imageParser.color
import ru.spbstu.icfpc2022.imageParser.euclid
import ru.spbstu.icfpc2022.imageParser.get
import ru.spbstu.icfpc2022.imageParser.getCanvasColor
import ru.spbstu.icfpc2022.imageParser.getOrNull
import ru.spbstu.icfpc2022.imageParser.parseImage
import ru.spbstu.icfpc2022.imageParser.point
import ru.spbstu.icfpc2022.imageParser.toImage
import ru.spbstu.icfpc2022.move.Move
import ru.spbstu.icfpc2022.robovinchi.StateCollector
import ru.spbstu.ktuples.zip
import ru.spbstu.wheels.resize
import java.util.TreeMap
import kotlin.math.round

data class Task(
    val problemId: Int,
    val targetImage: ImmutableImage,
    val initialCanvas: Canvas,
    val bestScore: Long? = null,
) {
    constructor(problemId: Int, initialConfig: InitialConfig) : this(
        problemId,
        parseImage("problems/$problemId.png"),
        Canvas(
            initialConfig.blocks.map { it.id as SimpleId }.maxOf { it.id },
            initialConfig.blocks.associateBy { it.id }.toPersistentMap(),
            initialConfig.width,
            initialConfig.height
        )
    )

    constructor(
        problemId: Int,
        targetImage: ImmutableImage,
        initialConfig: InitialConfig,
        bestScore: Long? = null,
    ) : this(
        problemId,
        targetImage,
        Canvas(
            initialConfig.blocks.map { it.id as SimpleId }.maxOf { it.id },
            initialConfig.blocks.associateBy { it.id }.toPersistentMap(),
            initialConfig.width,
            initialConfig.height
        ),
        bestScore
    )

    val bestScoreOrMax get() = (bestScore ?: Long.MAX_VALUE)

    val snapPoints = TreeMap<Int, TreeMap<Int, Point>>()

    fun addSnap(point: Point) {
        snapPoints.getOrPut(point.x) { TreeMap() }.put(point.y, point)
    }

    fun closestSnap(point: Point, inShape: Shape): Point? {
        val submap = snapPoints.subMap(
            inShape.lowerLeftInclusive.x,
            false,
            inShape.upperRightExclusive.x - 1,
            false
        )
        val xh = submap.ceilingEntry(point.x)
        val xl = submap.floorEntry(point.x)

        val xhsub = xh?.value?.subMap(
            inShape.lowerLeftInclusive.y,
            false,
            inShape.upperRightExclusive.y - 1,
            false
        )
        val cand1 = xhsub?.ceilingEntry(point.y)?.value
        val cand2 = xhsub?.floorEntry(point.y)?.value
        val xlsub = xl?.value?.subMap(
            inShape.lowerLeftInclusive.y,
            false,
            inShape.upperRightExclusive.y - 1,
            false
        )
        val cand3 = xlsub?.ceilingEntry(point.y)?.value
        val cand4 = xlsub?.floorEntry(point.y)?.value

        return setOfNotNull(cand1, cand2, cand3, cand4)
            .filter { it.isStrictlyInside(inShape.lowerLeftInclusive, inShape.upperRightExclusive) }
            .minByOrNull { point.distance(it) }
    }

    init {
        for (pixel in targetImage) {
            val nextRight = targetImage.getOrNull(pixel.x + 1, pixel.y)
            val nextDown = targetImage.getOrNull(pixel.x, pixel.y + 1)
            if (nextRight != null && !AutocropTactic.approximatelyMatches(nextRight.color, pixel.color, 27))
                addSnap(pixel.point)
            if (nextDown != null && !AutocropTactic.approximatelyMatches(nextDown.color, pixel.color, 27))
                addSnap(pixel.point)
        }
    }
}

class PersistentState(
    val task: Task,
    val canvas: Canvas = Canvas.empty(task.targetImage.width, task.targetImage.height),
    val commands: PersistentList<Move> = persistentListOf(),
    val similarityCounter: PersistentSimilarity? = null,
    val cost: Long = 0L
) {
    val similarity: Double by lazy {
        val counter = similarityCounter ?: PersistentSimilarity.initial(canvas, task.targetImage)
        counter.similarity
    }

    val score: Long by lazy { round(similarity * 0.005).toLong() + cost }

    fun withIncrementalSimilarity() = PersistentState(
        task, canvas, commands,
        PersistentSimilarity.initial(canvas, task.targetImage),
        cost
    )

    fun move(move: Move, ignoreUI: Boolean = false): PersistentState {
        val newCost = cost + canvas.costOf(move)
        val newSimilarity = similarityCounter?.let { canvas.updateSimilarity(move, it) }
        val newCanvas = canvas.apply(move)
        return PersistentState(task, newCanvas, commands.add(move), newSimilarity, newCost)
            .also {
                if (!ignoreUI && !StateCollector.turnMeOff) StateCollector.commandToCanvas.add(
                    move to newCanvas.allSimpleBlocks().toList()
                )
            }
    }

    fun dumpSolution(): String = commands.joinToString("\n")
}

class PersistentSimilarity(
    val target: ImmutableImage,
    val pixelSimilarity: PersistentList<Double>,
    val similarity: Double
) {
    fun update(shape: Shape, color: Color): PersistentSimilarity {
        val pixelDiff = pixelSimilarity.builder()
        var newSimilarity = similarity
        for (x in shape.lowerLeftInclusive.x until shape.upperRightExclusive.x) {
            for (y in shape.lowerLeftInclusive.y until shape.upperRightExclusive.y) {
                val targetColor = target.get(x, y).getCanvasColor()
                val diff = euclid(
                    targetColor.r - color.r,
                    targetColor.g - color.g,
                    targetColor.b - color.b,
                    targetColor.a - color.a,
                )
                val index = computeIndexOfCoord(target.width, x, y)
                val oldDiff = pixelDiff[index]
                pixelDiff[index] = diff
                newSimilarity -= oldDiff
                newSimilarity += diff
            }
        }
        return PersistentSimilarity(target, pixelDiff.build(), newSimilarity)
    }

    companion object {
        private fun computeIndexOfCoord(width: Int, x: Int, y: Int) = x * width + y
        fun initial(canvas: Canvas, targetImage: ImmutableImage): PersistentSimilarity {
            val pixelSimilarity = persistentListOf<Double>().builder()
            pixelSimilarity.resize(targetImage.width * targetImage.height) { 0.0 }
            val canvasImage = canvas.toImage()
            val similarity =
                zip(targetImage.iterator().asSequence(), canvasImage.iterator().asSequence()) { target, cImage ->
                    check(target.x == cImage.x)
                    check(target.y == cImage.y)
                    val diff = euclid(
                        target.red() - cImage.red(),
                        target.green() - cImage.green(),
                        target.blue() - cImage.blue(),
                        target.alpha() - cImage.alpha()
                    )
                    pixelSimilarity[computeIndexOfCoord(targetImage.width, target.x, target.y)] = diff
                    diff
                }.sum()
            return PersistentSimilarity(targetImage, pixelSimilarity.build(), similarity)
        }
    }
}

val Task.initialState: PersistentState
    get() = PersistentState(
        this,
        initialCanvas
    )

abstract class Solver(
    val task: Task
) {
    abstract fun solve(): PersistentState
}

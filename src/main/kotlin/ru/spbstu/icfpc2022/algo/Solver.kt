package ru.spbstu.icfpc2022.algo

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.pixels.Pixel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
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
import java.util.TreeMap
import kotlin.math.round

class PointHolder {
    val pointData = TreeMap<Int, TreeMap<Int, Point>>()

    fun add(point: Point) {
        pointData.getOrPut(point.x) { TreeMap() }.put(point.y, point)
    }

    fun closestPointTo(point: Point, inShape: Shape): Point? {
        val submap = pointData.subMap(
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

    fun toSet() = pointData.flatMap { it.value.values }
}

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

    val snapPoints = PointHolder()
    val cornerSnapPoints = PointHolder()

    fun closestSnap(point: Point, inShape: Shape): Point? = snapPoints.closestPointTo(point, inShape)
    fun closestCornerSnap(point: Point, inShape: Shape): Point? = cornerSnapPoints.closestPointTo(point, inShape)

    inline val Pixel.below get() = targetImage.getOrNull(x, y + 1)
    inline val Pixel.above get() = targetImage.getOrNull(x, y - 1)
    inline val Pixel.right get() = targetImage.getOrNull(x + 1, y)
    inline val Pixel.left get() = targetImage.getOrNull(x - 1, y)



    init {
        infix fun Pixel?.notMatches(pixel2: Pixel?): Boolean {
            this ?: return false
            pixel2 ?: return false
            return !AutocropTactic.approximatelyMatches(color, pixel2.color, 27)
        }

        for (pixel in targetImage) {
            val nextRight = pixel.right
            if (nextRight notMatches pixel) {
                snapPoints.add(nextRight!!.point)
                if (pixel notMatches pixel.above || pixel notMatches pixel.below)
                    cornerSnapPoints.add(nextRight.point)
            }

            val nextUp = pixel.above
            if (nextUp notMatches pixel) {
                snapPoints.add(nextUp!!.point)
                if (pixel notMatches pixel.left || pixel notMatches pixel.right)
                    cornerSnapPoints.add(nextUp.point)
            }
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
    val pixelSimilarity: PersistentMap<Pair<Int, Int>, Double>,
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
                val oldDiff = pixelDiff.put(x to y, diff)
                    ?: error("no diff for pixel (${x}, ${y})")
                newSimilarity -= oldDiff
                newSimilarity += diff
            }
        }
        return PersistentSimilarity(target, pixelDiff.build(), newSimilarity)
    }

    companion object {
        fun initial(canvas: Canvas, targetImage: ImmutableImage): PersistentSimilarity {
            val pixelSimilarity = persistentHashMapOf<Pair<Int, Int>, Double>().builder()
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
                    pixelSimilarity[target.x to target.y] = diff
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

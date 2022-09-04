package ru.spbstu.icfpc2022.algo.tactics

import com.sksamuel.scrimage.nio.PngWriter
import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.canvas.BlockId
import ru.spbstu.icfpc2022.canvas.Color
import ru.spbstu.icfpc2022.canvas.Point
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.icfpc2022.imageParser.euclid
import ru.spbstu.icfpc2022.imageParser.get
import ru.spbstu.icfpc2022.imageParser.getCanvasColor
import ru.spbstu.icfpc2022.imageParser.toImage
import ru.spbstu.icfpc2022.move.ColorMove
import ru.spbstu.icfpc2022.move.LineCutMove
import ru.spbstu.icfpc2022.move.Orientation
import ru.spbstu.icfpc2022.move.PointCutMove
import java.io.File

class OverlayCropTactic(
    task: Task,
    tacticStorage: TacticStorage,
    val colorToleranceInt: Int,
) : Tactic(task, tacticStorage) {
    val colorTolerance = colorToleranceInt.toDouble()

    private val Point.pixel get() = task.targetImage[x, y]

    private val Shape.allPoints: List<Point>
        get() {
            val points = mutableListOf<Point>()
            for (x in lowerLeftInclusive.x until upperRightExclusive.x) {
                for (y in lowerLeftInclusive.y until upperRightExclusive.y) {
                    points += Point(x, y)
                }
            }
            return points
        }

    private val pointDeltas = listOf(
        Point(-1, -1),
        Point(0, -1),
        Point(1, -1),
        Point(-1, 0),
        Point(1, 0),
        Point(1, -1),
        Point(1, 1),
        Point(0, 1)
    )

    private val Point.neighbors get() = pointDeltas.map { this + it }

    private fun Color.distance(other: Color): Double = euclid(
        r - other.r, g - other.g, b - other.b, a - other.a
    )


    private val Point.index: Int get() = x * task.targetImage.width + y

    private fun getBoundingBox(shape: Shape, point: Point): Pair<Shape, BooleanArray> {
        val targetColor = point.pixel.getCanvasColor()
        val queue = ArrayDeque<Point>()
        queue.add(point)
        val points = ArrayList<Point>(shape.size.toInt())
        val visitedPoints = BooleanArray(task.targetImage.width * task.targetImage.height) { false }
        while (queue.isNotEmpty()) {
            val currentPoint = queue.removeFirst()
            val currentPointIndex = currentPoint.index
            if (visitedPoints[currentPointIndex]) continue
            visitedPoints[currentPointIndex] = true
            points.add(currentPoint)

            val availableNeighbours = currentPoint.neighbors
                .filter { it.isStrictlyInsideOrLeft(shape.lowerLeftInclusive, shape.upperRightExclusive) }
                .filterNot { visitedPoints[it.index] }
                .filter { it.pixel.getCanvasColor().distance(targetColor) <= colorTolerance }
            queue.addAll(availableNeighbours)
        }

        val minX = points.minOf { it.x }
        val minY = points.minOf { it.y }
        val maxXExclusive = points.maxOf { it.x + 1 }
        val maxYExclusive = points.maxOf { it.y + 1 }
        return Shape(Point(minX, minY), Point(maxXExclusive, maxYExclusive)) to visitedPoints
    }


    override fun invoke(state: PersistentState): PersistentState {
        var blockId = state.canvas.blocks.keys.single()
//        val backgroundColor = storage.get<ColorBackgroundTactic>()?.resultingColor!!

        val currentBlock = state.canvas.blocks[blockId]!!
        val coloredPoints = currentBlock.shape.allPoints//.filterNot {
//            it.pixel.getCanvasColor().distance(backgroundColor) < colorTolerance
//        }

        val globalVisitedPoints = BooleanArray(task.targetImage.width * task.targetImage.height) { false }
        val shapeColors = mutableMapOf<Shape, Color>()
        for (point in coloredPoints) {
            if (globalVisitedPoints[point.index]) continue
            val (shape, visited) = getBoundingBox(currentBlock.shape, point)
            shapeColors[shape] = point.pixel.getCanvasColor()
            for ((index, value) in visited.withIndex()) {
                globalVisitedPoints[index] = globalVisitedPoints[index] or value
            }
        }

        val sortedShapes = shapeColors.keys.sortedBy { it.size }.toMutableList()
        val shapeLevels = mutableListOf<MutableList<Shape>>(mutableListOf())
        while (sortedShapes.isNotEmpty()) {
            val shape = sortedShapes.removeLast()
            propagateShape(shape, shapeLevels)
        }


        var currentState = state

        for (shapeLevel in shapeLevels) {
            for (shape in shapeLevel) {

                val shapeColor = shapeColors[shape]!!
                val (cuttedState, colorBlockId) = cutOutShape(currentState, blockId, shape)

                currentState = cuttedState.move(ColorMove(colorBlockId, shapeColor))

                currentState = MergeToOneTactic(task, storage)(currentState)
                blockId = currentState.canvas.blocks.keys.single()
            }
        }

        return currentState
    }

    private enum class BlockLocation {
        BOTTOM_LEFT, UPPER_RIGHT
    }

    private fun cutOutShape(state: PersistentState, blockId: BlockId, shape: Shape): Pair<PersistentState, BlockId> {
        var currentState = state
        var currentBlockId = blockId
        var currentBlock = state.canvas.blocks[blockId]!!
        var currentBlockShape = currentBlock.shape


        val cutPoints = listOf(
            Triple(
                shape.lowerLeftInclusive,
                Shape(currentBlockShape.lowerLeftInclusive, shape.lowerLeftInclusive).size,
                BlockLocation.UPPER_RIGHT
            ),
            Triple(
                shape.upperRightExclusive,
                Shape(shape.upperRightExclusive, currentBlockShape.upperRightExclusive).size,
                BlockLocation.BOTTOM_LEFT
            )
        ).sortedByDescending { it.second }.map { it.first to it.third }

        for ((cutPoint, blockLocation) in cutPoints) {
            val (cutMove, nextBlockId) = when {
                cutPoint == currentBlockShape.lowerLeftInclusive || cutPoint == currentBlockShape.upperRightExclusive -> {
                    continue
                }

                cutPoint.x == currentBlockShape.lowerLeftInclusive.x || cutPoint.x == currentBlockShape.upperRightExclusive.x -> LineCutMove(
                    currentBlockId,
                    Orientation.Y,
                    cutPoint.y
                ) to when (blockLocation) {
                    BlockLocation.BOTTOM_LEFT -> currentBlockId + 0
                    BlockLocation.UPPER_RIGHT -> currentBlockId + 1
                }

                cutPoint.y == currentBlockShape.lowerLeftInclusive.y || cutPoint.y == currentBlockShape.upperRightExclusive.y -> LineCutMove(
                    currentBlockId,
                    Orientation.X,
                    cutPoint.x
                ) to when (blockLocation) {
                    BlockLocation.BOTTOM_LEFT -> currentBlockId + 0
                    BlockLocation.UPPER_RIGHT -> currentBlockId + 1
                }

                else -> PointCutMove(currentBlockId, cutPoint) to when (blockLocation) {
                    BlockLocation.BOTTOM_LEFT -> currentBlockId + 0
                    BlockLocation.UPPER_RIGHT -> currentBlockId + 2
                }
            }

            currentState = currentState.move(cutMove)
            currentBlockId = nextBlockId
            currentBlock = currentState.canvas.blocks[currentBlockId]!!
            currentBlockShape = currentBlock.shape
        }

        return currentState to currentBlockId
    }

    private fun propagateShape(shape: Shape, shapeLevels: MutableList<MutableList<Shape>>) {
        for ((level, levelShapes) in shapeLevels.withIndex().reversed()) {
            if (!checkShapeOverlapping(shape, levelShapes)) continue
            if (level + 1 >= shapeLevels.size) shapeLevels.add(mutableListOf())
            shapeLevels[level + 1].add(shape)
            return
        }
        shapeLevels[0].add(shape)
    }

    private fun Shape.overlaps(other: Shape): Boolean {
        return lowerLeftInclusive.x < other.lowerLeftInclusive.x + other.width
                && lowerLeftInclusive.x + width > other.lowerLeftInclusive.x
                && lowerLeftInclusive.y < other.lowerLeftInclusive.y + other.height
                && lowerLeftInclusive.y + height > other.lowerLeftInclusive.y
    }

    private fun checkShapeOverlapping(shape: Shape, shapes: List<Shape>): Boolean = shapes.any { shape.overlaps(it) }
}

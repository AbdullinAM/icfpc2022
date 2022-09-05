package ru.spbstu.icfpc2022.algo.tactics

import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.canvas.*
import ru.spbstu.icfpc2022.imageParser.euclid
import ru.spbstu.icfpc2022.imageParser.get
import ru.spbstu.icfpc2022.imageParser.getCanvasColor
import ru.spbstu.icfpc2022.move.ColorMove
import ru.spbstu.icfpc2022.move.LineCutMove
import ru.spbstu.icfpc2022.move.Orientation
import ru.spbstu.icfpc2022.move.PointCutMove

class RandomRectangleCropTactic(
    task: Task,
    storage: TacticStorage,
    val colorToleranceInt: Int,
    val pixelTolerance: Double,
    val limit: Long,
    val coloringMethod: ColoringMethod
) : Tactic(task, storage,) {
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

    private fun getBoundingBox(boundingBox: Shape, point: Point): Shape {
        val targetColor = point.pixel.getCanvasColor()
        var potentialShape = Shape(point, point + Point(1 , 1))
        while (true) {
            var hasGrown = false
            if (potentialShape.upperRightExclusive.y < boundingBox.upperRightExclusive.y) {
                val upperLine = (potentialShape.lowerLeftInclusive.x until potentialShape.upperRightExclusive.x).map {
                    Point(it, potentialShape.upperRightExclusive.y).pixel
                }
                if (upperLine.map { it.getCanvasColor() }.count { it.distance(targetColor) <= colorTolerance } >= (upperLine.size * pixelTolerance)) {
                    potentialShape = Shape(potentialShape.lowerLeftInclusive, potentialShape.upperRightExclusive + Point(0, 1))
                    hasGrown = true
                }
            }
            if (potentialShape.upperRightExclusive.x < boundingBox.upperRightExclusive.x) {
                val righterLine = (potentialShape.lowerLeftInclusive.y until potentialShape.upperRightExclusive.y).map {
                    Point(potentialShape.upperRightExclusive.x, it).pixel
                }
                if (righterLine.map { it.getCanvasColor() }.count { it.distance(targetColor) <= colorTolerance } >= (righterLine.size * pixelTolerance)) {
                    potentialShape = Shape(potentialShape.lowerLeftInclusive, potentialShape.upperRightExclusive + Point(1, 0))
                    hasGrown = true
                }
            }


            if (potentialShape.lowerLeftInclusive.y > boundingBox.lowerLeftInclusive.y) {
                val lowerLine = (potentialShape.lowerLeftInclusive.x until potentialShape.upperRightExclusive.x).map {
                    Point(it, potentialShape.lowerLeftInclusive.y - 1).pixel
                }
                if (lowerLine.map { it.getCanvasColor() }.count { it.distance(targetColor) <= colorTolerance } >= (lowerLine.size * pixelTolerance)) {
                    potentialShape = Shape(potentialShape.lowerLeftInclusive - Point(0, 1), potentialShape.upperRightExclusive)
                    hasGrown = true
                }
            }

            if (potentialShape.lowerLeftInclusive.x > boundingBox.lowerLeftInclusive.x) {
                val lefterLine = (potentialShape.lowerLeftInclusive.y until potentialShape.upperRightExclusive.y).map {
                    Point(potentialShape.lowerLeftInclusive.x - 1, it).pixel
                }
                if (lefterLine.map { it.getCanvasColor() }.count { it.distance(targetColor) <= colorTolerance } >= (lefterLine.size * pixelTolerance)) {
                    potentialShape = Shape(potentialShape.lowerLeftInclusive - Point(1, 0), potentialShape.upperRightExclusive)
                    hasGrown = true
                }
            }

            if (!hasGrown) break
        }
        return potentialShape
    }


    override fun invoke(state: PersistentState): PersistentState {
        var blockId = state.canvas.blocks.keys.single()
        val currentBlock = state.canvas.blocks[blockId]!!
        val coloredPoints = currentBlock.shape.allPoints.toList()

        return (0..7).map { doTheWork(state, currentBlock, coloredPoints.shuffled().toMutableSet()) }.minBy { it.score }
    }

    fun doTheWork(state: PersistentState, currentBlock: Block, coloredPoints: MutableSet<Point>): PersistentState {
        var blockId = currentBlock.id
        val shapeColors = mutableMapOf<Shape, Color>()
        while (coloredPoints.isNotEmpty()) {
            val randomPoint = coloredPoints.random()
            val shape = getBoundingBox(currentBlock.shape, randomPoint)
            if (shape.size < limit) {
                coloredPoints.remove(randomPoint)
                continue
            }
            shapeColors[shape] = randomPoint.pixel.getCanvasColor()
            coloredPoints.removeAll(shape.allPoints.toSet())
        }

        val sortedShapes = shapeColors.keys.sortedByDescending { it.size }

        var currentState = state


        for (shape in sortedShapes) {

            val (cuttedState, colorBlockId) = cutOutShape(currentState, blockId, shape)

            var newState = colorBlockToAverageBest(cuttedState, colorBlockId, colorToleranceInt)

            newState = MergeToOneTactic(task, storage)(newState)
            if (newState.score > currentState.score) continue
            currentState = newState
            blockId = currentState.canvas.blocks.keys.single()
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

}
package ru.spbstu.icfpc2022.algo.tactics

import com.sksamuel.scrimage.ImmutableImage
import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.algo.initialState
import ru.spbstu.icfpc2022.algo.tactics.AutocropTactic.Companion.autocrop
import ru.spbstu.icfpc2022.canvas.BlockId
import ru.spbstu.icfpc2022.canvas.Color
import ru.spbstu.icfpc2022.canvas.Point
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.icfpc2022.canvas.SimpleId
import ru.spbstu.icfpc2022.imageParser.subimage
import ru.spbstu.icfpc2022.move.ColorMove
import ru.spbstu.icfpc2022.move.LineCutMove
import ru.spbstu.icfpc2022.move.Orientation
import ru.spbstu.icfpc2022.move.PointCutMove
import kotlin.random.Random

class AutocropTactic2(task: Task, tacticStorage: TacticStorage, val colorTolerance: Int) :
    BlockTactic(task, tacticStorage) {

    val defaultPixelTolerance = 0.95
    val defaultWidth = 10

    lateinit var finalStates: List<Pair<BlockId, PersistentState>>

    fun Point.fitInto(shape: Shape) = Point(
        x.coerceAtLeast(0).coerceAtMost(shape.width - 1),
        y.coerceAtLeast(0).coerceAtMost(shape.height - 1),
    )

    override fun invoke(state: PersistentState, blockId: BlockId): PersistentState {
        val cropBlock = state.canvas.blocks[blockId]!!
        val resultStates = arrayListOf<AutocropTactic.Companion.AutocropState>()
        val states = arrayListOf<Pair<Int, AutocropTactic.Companion.AutocropState>>()
        states.add(
            0 to AutocropTactic.Companion.AutocropState(
                state,
                task.targetImage.subimage(cropBlock.shape),
                SimpleId(0)
            )
        )

        while (states.isNotEmpty()) {
            if (resultStates.size > 20000) break
            val idx = Random.nextInt(states.size)
            val (depth, autocropState) = states.removeAt(idx)
            if (depth > 20) {
                continue
            }
            val shape = autocropState.state.canvas.blocks[autocropState.block]!!.shape
            if (shape.size < 40 * 40) {
                resultStates.add(autocropState)
                continue
            }

            var tolerance = colorTolerance
            var pixelTolerance = defaultPixelTolerance
            var width = defaultWidth

//            val sizeLimit = task.targetImage.width * task.targetImage.height * 0.07
            val sizeLimit = minOf(
                shape.size * 0.15,
                task.targetImage.width * task.targetImage.height * 0.07
            )
            val crops = arrayListOf<Triple<Color, Shape, ImmutableImage>>()
            while (crops.size < 30) {
                val variants = mutableListOf(
                    Point(0, 0) to Point(shape.width, width),
                    Point(0, 0) to Point(width, shape.height),
                    Point(0, shape.height - width) to Point(shape.width, shape.height),
                    Point(shape.width - width, 0) to Point(shape.width, shape.height),
                ).map { (p1, p2) -> p1.fitInto(shape) to p2.fitInto(shape) }

                val colors = variants.map {
                    computeBlockAverage(autocropState.image, Shape(it.first, it.second))
                }
                val autocrops = colors.map { it to autocrop(autocropState.image, it, tolerance, pixelTolerance) }
                    .filter { it.second.first != null }
                    .map { Triple(it.first, it.second.first!!, it.second.second) }
                    .filter { (shape.size - it.second.size) > sizeLimit }
                var changed = false
                if (tolerance < 100) {
                    tolerance++
                    changed = true
                }
                if (pixelTolerance > 0) {
                    pixelTolerance -= 0.01
                    changed = true
                }
                if (width < 100 && pixelTolerance < 0.5) {
                    width += 10
                    tolerance = colorTolerance
                    pixelTolerance = defaultPixelTolerance
                    changed = true
                }
                if (!changed) {
                    println("break: $tolerance $pixelTolerance $width")
                    break
                }
                crops += autocrops
            }

            for (bestCrop in crops) {
                val colorMove = ColorMove(autocropState.block, bestCrop.first)
                var newState = autocropState.state.move(colorMove)

                val newBlockShape = Shape(
                    bestCrop.second.lowerLeftInclusive.add(shape.lowerLeftInclusive),
                    bestCrop.second.upperRightExclusive.add(shape.lowerLeftInclusive),
                )

                var nextBlock = autocropState.block

                if (newBlockShape.lowerLeftInclusive != shape.lowerLeftInclusive) {
                    val firstCrop = when {
                        newBlockShape.lowerLeftInclusive.x == shape.lowerLeftInclusive.x -> LineCutMove(
                            nextBlock,
                            Orientation.Y,
                            newBlockShape.lowerLeftInclusive.y
                        )
                        newBlockShape.lowerLeftInclusive.y == shape.lowerLeftInclusive.y -> LineCutMove(
                            nextBlock,
                            Orientation.X,
                            newBlockShape.lowerLeftInclusive.x
                        )
                        else -> PointCutMove(nextBlock, newBlockShape.lowerLeftInclusive)
                    }
                    newState = newState.move(firstCrop)

                    nextBlock = newState.canvas.blocks.toList().firstOrNull {
                        newBlockShape.middle.isStrictlyInside(
                            it.second.shape.lowerLeftInclusive,
                            it.second.shape.upperRightExclusive
                        )
                    }?.first.also {
                        if (it == null) System.err.println("cut failed")
                    } ?: continue
                }

                if (newBlockShape.upperRightExclusive != shape.upperRightExclusive) {
                    val secondCrop = when {
                        newBlockShape.upperRightExclusive.x == shape.upperRightExclusive.x -> LineCutMove(
                            nextBlock,
                            Orientation.Y,
                            newBlockShape.upperRightExclusive.y - 1
                        )
                        newBlockShape.upperRightExclusive.y == shape.upperRightExclusive.y -> LineCutMove(
                            nextBlock,
                            Orientation.X,
                            newBlockShape.upperRightExclusive.x - 1
                        )
                        else -> PointCutMove(nextBlock, newBlockShape.upperRightExclusive - Point(1, 1))
                    }
                    newState = newState.move(secondCrop)
                    nextBlock = newState.canvas.blocks.toList().firstOrNull {
                        newBlockShape.middle.isStrictlyInside(
                            it.second.shape.lowerLeftInclusive,
                            it.second.shape.upperRightExclusive
                        )
                    }?.first.also {
                        if (it == null) System.err.println("cut failed")
                    } ?: continue
                }

                val finalState = AutocropTactic.Companion.AutocropState(
                    newState,
                    bestCrop.third,
                    nextBlock,
                )
                if (nextBlock != autocropState.block && finalState.state.canvas.blocks[nextBlock]!!.shape.size > 40 * 40) {
                    states.add(depth + 1 to finalState)
                }
                resultStates.add(finalState)
            }
        }
        finalStates = resultStates.map { it.block to it.state }
        return task.initialState
    }
}

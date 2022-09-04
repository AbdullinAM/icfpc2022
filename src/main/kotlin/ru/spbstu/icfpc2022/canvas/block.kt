package ru.spbstu.icfpc2022.canvas

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import ru.spbstu.icfpc2022.algo.PersistentSimilarity
import ru.spbstu.icfpc2022.move.ColorMove
import ru.spbstu.icfpc2022.move.LineCutMove
import ru.spbstu.icfpc2022.move.MergeMove
import ru.spbstu.icfpc2022.move.Move
import ru.spbstu.icfpc2022.move.Orientation
import ru.spbstu.icfpc2022.move.PointCutMove
import ru.spbstu.icfpc2022.move.SwapMove
import kotlin.math.round
import kotlin.math.sqrt


data class Point(
    val x: Int,
    val y: Int
) {
    override fun toString(): String = "[$x, $y]"

    fun add(other: Point) = Point(x + other.x, y + other.y)
    fun subtract(other: Point) = Point(x - other.x, y - other.y)
    fun distance(other: Point) = sqrt((x.toDouble() - other.x).let { it * it } + (y.toDouble() - other.y).let { it * it })


    operator fun plus(other: Point) = this.add(other)
    operator fun minus(other: Point) = this.subtract(other)

    fun isStrictlyInside(bottomLeft: Point, topRight: Point): Boolean =
        bottomLeft.x < x
                && x < topRight.x
                && bottomLeft.y < y
                && y < topRight.y

    fun isOnBoundary(bottomLeft: Point, topRight: Point): Boolean =
        (bottomLeft.x == x && bottomLeft.y <= y && y <= topRight.y)
                || (topRight.x == x && bottomLeft.y <= y && y <= topRight.y)
                || (bottomLeft.y == y && bottomLeft.x <= x && x <= topRight.x)
                || (topRight.y == y && bottomLeft.x <= x && x <= topRight.x)

    fun isInside(bottomLeft: Point, topRight: Point): Boolean =
        isStrictlyInside(bottomLeft, topRight) || isOnBoundary(bottomLeft, topRight)

    fun midPointWith(that: Point) = Point((x + that.x) / 2, (y + that.y) / 2)
}

data class Color(
    val r: Int,
    val g: Int,
    val b: Int,
    val a: Int
) {
    override fun toString(): String = "[$r, $g, $b, $a]"
}

data class Shape(
    val lowerLeftInclusive: Point,
    val upperRightExclusive: Point
) {
    val width: Int get() = upperRightExclusive.x - lowerLeftInclusive.x
    val height: Int get() = upperRightExclusive.y - lowerLeftInclusive.y

    val size: Long get() = width.toLong() * height.toLong()

    val middle: Point = Point(lowerLeftInclusive.x + width / 2, lowerLeftInclusive.y + height / 2)

    fun isVerticallyAligned(other: Shape) =
        (lowerLeftInclusive.y == other.upperRightExclusive.y
                || upperRightExclusive.y == other.lowerLeftInclusive.y)
                && lowerLeftInclusive.x == other.lowerLeftInclusive.x
                && upperRightExclusive.x == other.upperRightExclusive.x

    fun isHorizontallyAligned(other: Shape) =
        (lowerLeftInclusive.x == other.upperRightExclusive.x
                || upperRightExclusive.x == other.lowerLeftInclusive.x)
                && lowerLeftInclusive.y == other.lowerLeftInclusive.y
                && upperRightExclusive.y == other.upperRightExclusive.y

}

sealed class BlockId {
    operator fun plus(number: Int): BlockId = ComplexId(this, number)
}

data class SimpleId(val id: Int) : BlockId() {
    override fun toString(): String = "$id"
}

data class ComplexId(
    val parent: BlockId,
    val id: Int
) : BlockId() {
    override fun toString(): String = "$parent.$id"
}


sealed class Block {
    abstract val id: BlockId
    abstract val shape: Shape

    abstract fun simpleChildren(): Sequence<SimpleBlock>
}

data class SimpleBlock(
    override val id: BlockId,
    override val shape: Shape,
    val color: Color
) : Block() {
    init {
        check(shape.size > 0L) { "empty blocks are prohibited" }
    }

    override fun simpleChildren(): Sequence<SimpleBlock> = sequenceOf(this)
}

data class ComplexBlock(
    override val id: BlockId,
    override val shape: Shape,
    val children: Set<SimpleBlock>
) : Block() {
    init {
        check(shape.size > 0L) { "empty blocks are prohibited" }
    }

    override fun simpleChildren(): Sequence<SimpleBlock> = children.asSequence()
}


data class Canvas(
    val blockId: Int,
    val blocks: PersistentMap<BlockId, Block>,
    val width: Int,
    val height: Int
) {
    val size: Long get() = width.toLong() * height.toLong()

    fun allSimpleBlocks(): Sequence<SimpleBlock> = blocks.values.asSequence().flatMap {
        when (it) {
            is SimpleBlock -> sequenceOf(it)
            is ComplexBlock -> it.simpleChildren()
        }
    }

    fun apply(move: Move): Canvas = when (move) {
        is LineCutMove -> {
            val block = blocks[move.block]!!
            when (move.orientation) {
                Orientation.X -> verticalCut(block, move.offset)
                Orientation.Y -> horizontalCut(block, move.offset)
            }
        }

        is ColorMove -> {
            val block = blocks[move.block]!!
            val newBlock = when (block) {
                is SimpleBlock -> block.copy(color = move.color)
                is ComplexBlock -> SimpleBlock(block.id, block.shape, move.color)
            }
            Canvas(blockId, blocks.put(block.id, newBlock), width, height)
        }

        is MergeMove -> {
            val first = blocks[move.first]!!
            val second = blocks[move.second]!!

            check(first.shape.isVerticallyAligned(second.shape) || first.shape.isHorizontallyAligned(second.shape)) {
                "merge blocks are not aligned"
            }

            val lowerX = minOf(first.shape.lowerLeftInclusive.x, second.shape.lowerLeftInclusive.x)
            val lowerY = minOf(first.shape.lowerLeftInclusive.y, second.shape.lowerLeftInclusive.y)
            val upperX = maxOf(first.shape.upperRightExclusive.x, second.shape.upperRightExclusive.x)
            val upperY = maxOf(first.shape.upperRightExclusive.y, second.shape.upperRightExclusive.y)

            val children = mutableSetOf<SimpleBlock>()
            children.addAll(first.simpleChildren())
            children.addAll(second.simpleChildren())

            val complex = ComplexBlock(
                SimpleId(blockId + 1),
                Shape(Point(lowerX, lowerY), Point(upperX, upperY)),
                children
            )
            val newBlocks = blocks.builder()
            newBlocks.remove(first.id)
            newBlocks.remove(second.id)
            newBlocks[complex.id] = complex

            Canvas(
                blockId + 1,
                newBlocks.build(), width, height
            )
        }

        is PointCutMove -> pointCut(move)
        is SwapMove -> swap(move)
    }

    private fun verticalCut(block: Block, offset: Int): Canvas = when (block) {
        is SimpleBlock -> {
            val leftBlock = SimpleBlock(
                block.id + 0,
                Shape(block.shape.lowerLeftInclusive, Point(offset, block.shape.upperRightExclusive.y)),
                block.color
            )
            val rightBlock = SimpleBlock(
                block.id + 1,
                Shape(Point(offset, block.shape.lowerLeftInclusive.y), block.shape.upperRightExclusive),
                block.color
            )

            val newBlocks = blocks.builder()
            newBlocks.remove(block.id)
            newBlocks[leftBlock.id] = leftBlock
            newBlocks[rightBlock.id] = rightBlock

            Canvas(blockId, newBlocks.build(), width, height)
        }

        is ComplexBlock -> {
            val leftBlocks = mutableSetOf<SimpleBlock>()
            val rightBlocks = mutableSetOf<SimpleBlock>()
            for (child in block.children) {
                when {
                    child.shape.lowerLeftInclusive.x >= offset -> {
                        rightBlocks += child
                    }

                    child.shape.upperRightExclusive.x <= offset -> {
                        leftBlocks += child
                    }

                    else -> {
                        leftBlocks += SimpleBlock(
                            child.id + 0,
                            Shape(child.shape.lowerLeftInclusive, Point(offset, child.shape.upperRightExclusive.y)),
                            child.color
                        )
                        rightBlocks += SimpleBlock(
                            child.id + 1,
                            Shape(Point(offset, child.shape.lowerLeftInclusive.y), child.shape.upperRightExclusive),
                            child.color
                        )
                    }
                }
            }
            val leftBlock = ComplexBlock(
                block.id + 0,
                Shape(block.shape.lowerLeftInclusive, Point(offset, block.shape.upperRightExclusive.y)),
                leftBlocks
            )
            val rightBlock = ComplexBlock(
                block.id + 1,
                Shape(Point(offset, block.shape.lowerLeftInclusive.y), block.shape.upperRightExclusive),
                rightBlocks
            )

            val newBlocks = blocks.builder()
            newBlocks.remove(block.id)
            newBlocks[leftBlock.id] = leftBlock
            newBlocks[rightBlock.id] = rightBlock
            Canvas(blockId, newBlocks.build(), width, height)
        }
    }


    private fun horizontalCut(block: Block, offset: Int): Canvas = when (block) {
        is SimpleBlock -> {
            val leftBlock = SimpleBlock(
                block.id + 0,
                Shape(block.shape.lowerLeftInclusive, Point(block.shape.upperRightExclusive.x, offset)),
                block.color
            )
            val rightBlock = SimpleBlock(
                block.id + 1,
                Shape(Point(block.shape.lowerLeftInclusive.x, offset), block.shape.upperRightExclusive),
                block.color
            )

            val newBlocks = blocks.builder()
            newBlocks.remove(block.id)
            newBlocks[leftBlock.id] = leftBlock
            newBlocks[rightBlock.id] = rightBlock

            Canvas(blockId, newBlocks.build(), width, height)
        }

        is ComplexBlock -> {
            val bottomBlocks = mutableSetOf<SimpleBlock>()
            val topBlocks = mutableSetOf<SimpleBlock>()
            for (child in block.children) {
                when {
                    child.shape.lowerLeftInclusive.y >= offset -> {
                        topBlocks += child
                    }

                    child.shape.upperRightExclusive.y <= offset -> {
                        bottomBlocks += child
                    }

                    else -> {
                        bottomBlocks += SimpleBlock(
                            child.id + 0,
                            Shape(child.shape.lowerLeftInclusive, Point(child.shape.upperRightExclusive.x, offset)),
                            child.color
                        )
                        topBlocks += SimpleBlock(
                            child.id + 1,
                            Shape(Point(child.shape.lowerLeftInclusive.x, offset), child.shape.upperRightExclusive),
                            child.color
                        )
                    }
                }
            }
            val bottomBlock = ComplexBlock(
                block.id + 0,
                Shape(block.shape.lowerLeftInclusive, Point(block.shape.upperRightExclusive.x, offset)),
                bottomBlocks
            )
            val topBlock = ComplexBlock(
                block.id + 1,
                Shape(Point(block.shape.lowerLeftInclusive.x, offset), block.shape.upperRightExclusive),
                topBlocks
            )

            val newBlocks = blocks.builder()
            newBlocks.remove(block.id)
            newBlocks[bottomBlock.id] = bottomBlock
            newBlocks[topBlock.id] = topBlock
            Canvas(blockId, newBlocks.build(), width, height)
        }
    }

    private inline fun pointCutHelper(
        blockId: BlockId,
        block: Block,
        point: Point,
        bottomLeftBuilder: (BlockId, Shape) -> Block,
        bottomRightBuilder: (BlockId, Shape) -> Block,
        topLeftBuilder: (BlockId, Shape) -> Block,
        topRightBuilder: (BlockId, Shape) -> Block,
    ): Canvas {
        val bottomLeftBlock = bottomLeftBuilder(
            ComplexId(blockId, 0),
            Shape(block.shape.lowerLeftInclusive, point),
        )
        val bottomRightBlock = bottomRightBuilder(
            ComplexId(blockId, 1),
            Shape(
                Point(point.x, block.shape.lowerLeftInclusive.y),
                Point(block.shape.upperRightExclusive.x, point.y)
            )
        )
        val topRightBlock = topRightBuilder(
            ComplexId(blockId, 2),
            Shape(point, block.shape.upperRightExclusive)
        )
        val topLeftBlock = topLeftBuilder(
            ComplexId(blockId, 3),
            Shape(
                Point(block.shape.lowerLeftInclusive.x, point.y),
                Point(point.x, block.shape.upperRightExclusive.y)
            )
        )
        val newBlocks = with(blocks.builder()) {
            remove(blockId)
            put(bottomLeftBlock.id, bottomLeftBlock)
            put(bottomRightBlock.id, bottomRightBlock)
            put(topRightBlock.id, topRightBlock)
            put(topLeftBlock.id, topLeftBlock)
            build()
        }
        return Canvas(this.blockId, newBlocks, width, height)
    }

    private fun pointCut(move: PointCutMove): Canvas {
        val blockId = move.block
        val block = blocks[blockId] ?: error("no block $blockId")
        val point = move.offset.also {
            check(it.isStrictlyInside(block.shape.lowerLeftInclusive, block.shape.upperRightExclusive)) {
                "point $it is out of block $block"
            }
        }
        if (block is SimpleBlock) {
            return pointCutHelper(
                blockId = blockId, block = block, point = point,
                bottomLeftBuilder = { id, shape -> SimpleBlock(id, shape, block.color) },
                bottomRightBuilder = { id, shape -> SimpleBlock(id, shape, block.color) },
                topLeftBuilder = { id, shape -> SimpleBlock(id, shape, block.color) },
                topRightBuilder = { id, shape -> SimpleBlock(id, shape, block.color) }
            )
        }

        if (block is ComplexBlock) {
            val bottomLeftBlocks = hashSetOf<SimpleBlock>()
            val bottomRightBlocks = hashSetOf<SimpleBlock>()
            val topRightBlocks = hashSetOf<SimpleBlock>()
            val topLeftBlocks = hashSetOf<SimpleBlock>()
            block.children.forEach { subBlock ->
                /**
                 * __________________________
                 * |        |       |       |
                 * |   1    |   2   |   3   |
                 * |________|_______|_______|
                 * |        |       |       |
                 * |   4    |   5   |  6    |
                 * |________|_______|_______|
                 * |        |       |       |
                 * |   7    |   8   |   9   |
                 * |________|_______|_______|
                 */
                // Case 2
                if (subBlock.shape.lowerLeftInclusive.x >= point.x && subBlock.shape.lowerLeftInclusive.y >= point.y) {
                    topRightBlocks.add(subBlock)
                    return@forEach
                }
                // Case 7
                if (subBlock.shape.upperRightExclusive.x <= point.x && subBlock.shape.upperRightExclusive.y <= point.y) {
                    bottomLeftBlocks.add(subBlock)
                    return@forEach
                }
                // Case 1
                if (subBlock.shape.upperRightExclusive.x <= point.x && subBlock.shape.lowerLeftInclusive.y >= point.y) {
                    topLeftBlocks.add(subBlock)
                    return@forEach
                }
                // Case 9
                if (subBlock.shape.lowerLeftInclusive.x >= point.x && subBlock.shape.upperRightExclusive.y <= point.y) {
                    bottomRightBlocks.add(subBlock)
                    return@forEach
                }
                // Case 5
                if (point.isStrictlyInside(subBlock.shape.lowerLeftInclusive, subBlock.shape.upperRightExclusive)) {
                    bottomLeftBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 0), bottomLeftBlocks.size),
                            Shape(subBlock.shape.lowerLeftInclusive, point),
                            subBlock.color
                        )
                    )
                    bottomRightBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 1), bottomRightBlocks.size),
                            Shape(
                                Point(point.x, subBlock.shape.lowerLeftInclusive.y),
                                Point(subBlock.shape.upperRightExclusive.x, point.y)
                            ),
                            subBlock.color
                        )
                    )
                    topRightBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 2), topRightBlocks.size),
                            Shape(point, subBlock.shape.upperRightExclusive),
                            subBlock.color
                        )
                    )
                    topLeftBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 3), topLeftBlocks.size),
                            Shape(
                                Point(subBlock.shape.lowerLeftInclusive.x, point.y),
                                Point(point.x, subBlock.shape.upperRightExclusive.y)
                            ),
                            subBlock.color
                        )
                    )
                    return@forEach
                }

                // Case 2
                if (subBlock.shape.lowerLeftInclusive.x <= point.x
                    && point.x <= subBlock.shape.upperRightExclusive.x
                    && point.y <= subBlock.shape.lowerLeftInclusive.y
                ) {
                    topLeftBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 4), topLeftBlocks.size),
                            Shape(
                                subBlock.shape.lowerLeftInclusive,
                                Point(point.x, subBlock.shape.upperRightExclusive.y)
                            ),
                            subBlock.color
                        )
                    )
                    topRightBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 5), topRightBlocks.size),
                            Shape(
                                Point(point.x, subBlock.shape.lowerLeftInclusive.y),
                                subBlock.shape.upperRightExclusive
                            ),
                            subBlock.color
                        )
                    )
                    return@forEach
                }
                // Case 8
                if (subBlock.shape.lowerLeftInclusive.x <= point.x
                    && point.x <= subBlock.shape.upperRightExclusive.x
                    && point.y >= subBlock.shape.upperRightExclusive.y
                ) {
                    bottomLeftBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 6), bottomLeftBlocks.size),
                            Shape(
                                subBlock.shape.lowerLeftInclusive,
                                Point(point.x, subBlock.shape.upperRightExclusive.y)
                            ),
                            subBlock.color
                        )
                    )
                    bottomRightBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 7), bottomRightBlocks.size),
                            Shape(
                                Point(point.x, subBlock.shape.lowerLeftInclusive.y),
                                subBlock.shape.upperRightExclusive
                            ),
                            subBlock.color
                        )
                    )
                    return@forEach
                }
                // Case 4
                if (subBlock.shape.lowerLeftInclusive.y <= point.y
                    && point.y <= subBlock.shape.upperRightExclusive.y
                    && point.x <= subBlock.shape.lowerLeftInclusive.x
                ) {
                    bottomRightBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 8), bottomRightBlocks.size),
                            Shape(
                                subBlock.shape.lowerLeftInclusive,
                                Point(subBlock.shape.upperRightExclusive.x, point.y)
                            ),
                            subBlock.color
                        )
                    )
                    topRightBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 9), topRightBlocks.size),
                            Shape(
                                Point(subBlock.shape.lowerLeftInclusive.x, point.y),
                                subBlock.shape.upperRightExclusive
                            ),
                            subBlock.color
                        )
                    )
                    return@forEach
                }
                // Case 6
                if (subBlock.shape.lowerLeftInclusive.y <= point.y
                    && point.y <= subBlock.shape.upperRightExclusive.y
                    && point.x >= subBlock.shape.upperRightExclusive.x
                ) {
                    bottomLeftBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 10), bottomLeftBlocks.size),
                            Shape(
                                subBlock.shape.lowerLeftInclusive,
                                Point(subBlock.shape.upperRightExclusive.x, point.y)
                            ),
                            subBlock.color
                        )
                    )
                    topLeftBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 11), topLeftBlocks.size),
                            Shape(
                                Point(subBlock.shape.lowerLeftInclusive.x, point.y),
                                subBlock.shape.upperRightExclusive
                            ),
                            subBlock.color
                        )
                    )
                    return@forEach
                }
            }
            return pointCutHelper(
                blockId = blockId, block = block, point = point,
                bottomLeftBuilder = { id, shape -> ComplexBlock(id, shape, bottomLeftBlocks) },
                bottomRightBuilder = { id, shape -> ComplexBlock(id, shape, bottomRightBlocks) },
                topLeftBuilder = { id, shape -> ComplexBlock(id, shape, topRightBlocks) },
                topRightBuilder = { id, shape -> ComplexBlock(id, shape, topLeftBlocks) }
            )
        }
        error("unreachable")
    }

    private fun swap(move: SwapMove): Canvas {
        val block1 = blocks[move.first] ?: error("no block ${move.first}")
        val block2 = blocks[move.second] ?: error("no block ${move.second}")
        if (block1.shape.width != block2.shape.width || block1.shape.height != block2.shape.height) {
            error("block shape mismatch: ${block1.shape} : ${block2.shape}")
        }

        val newBlock1 = when (block1) {
            is SimpleBlock -> SimpleBlock(
                block1.id, block2.shape,
                block1.color
            )
            is ComplexBlock -> ComplexBlock(
                block1.id, block2.shape,
                block1.offsetChildren(block2.shape.lowerLeftInclusive)
            )
        }
        val newBlock2 = when (block2) {
            is SimpleBlock -> SimpleBlock(
                block2.id, block1.shape,
                block2.color
            )
            is ComplexBlock -> ComplexBlock(
                block2.id, block1.shape,
                block2.offsetChildren(block1.shape.lowerLeftInclusive)
            )
        }
        val newBlocks = with(blocks.builder()) {
            put(newBlock1.id, newBlock1)
            put(newBlock2.id, newBlock2)
            build()
        }
        return Canvas(blockId, newBlocks, width, height)
    }

    private fun ComplexBlock.offsetChildren(newLowerLeft: Point) =
        children.mapIndexed { i, block ->
            SimpleBlock(
                ComplexId(id, i),
                Shape(
                    block.shape.lowerLeftInclusive.add(newLowerLeft).subtract(shape.lowerLeftInclusive),
                    block.shape.upperRightExclusive.add(newLowerLeft).subtract(shape.lowerLeftInclusive)
                ),
                block.color
            )
        }.toSet()

    fun updateSimilarity(move: Move, similarity: PersistentSimilarity) = when (move) {
        is LineCutMove,
        is MergeMove,
        is PointCutMove -> similarity
        is ColorMove -> {
            val block = blocks[move.block]!!.shape
            similarity.update(block, move.color)
        }
        is SwapMove -> {
            val blocks = blocks[move.first]!!.simpleChildren() + blocks[move.second]!!.simpleChildren()
            blocks.fold(similarity) { sim, block -> sim.update(block.shape, block.color) }
        }
    }

    companion object {
        fun empty(width: Int, height: Int): Canvas {
            val mainBlockId = SimpleId(0)
            return Canvas(
                0,
                persistentMapOf(
                    mainBlockId to SimpleBlock(
                        mainBlockId,
                        Shape(Point(0, 0), Point(width - 1, height - 1)),
                        Color(255, 255, 255, 255)
                    )
                ),
                width, height
            )
        }
    }
}

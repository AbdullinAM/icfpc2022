package ru.spbstu.icfpc2022.canvas

import kotlinx.collections.immutable.PersistentMap
import ru.spbstu.icfpc2022.move.ColorMove
import ru.spbstu.icfpc2022.move.LineCutMove
import ru.spbstu.icfpc2022.move.MergeMove
import ru.spbstu.icfpc2022.move.Move
import ru.spbstu.icfpc2022.move.PointCutMove
import ru.spbstu.icfpc2022.move.SwapMove


data class Point(
    val x: Int,
    val y: Int
) {
    override fun toString(): String = "[$x, $y]"

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
}

data class Color(
    val r: Byte,
    val g: Byte,
    val b: Byte,
    val a: Byte
) {
    override fun toString(): String = "[$r, $g, $b, $a]"
}

data class Shape(
    val lowerLeft: Point,
    val upperRight: Point
) {
    val upperLeft: Point = Point(lowerLeft.x, upperRight.y)
    val lowerRight: Point = Point(upperRight.x, lowerLeft.y)

    val width: Int get() = upperRight.x - lowerLeft.x
    val height: Int get() = upperRight.y - lowerLeft.y
}

sealed class BlockId

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
    abstract fun withId(newId: BlockId): Block
}

data class SimpleBlock(
    override val id: BlockId,
    override val shape: Shape,
    val color: Color
) : Block() {
    override fun withId(newId: BlockId): Block = SimpleBlock(newId, shape, color)
}

data class ComplexBlock(
    override val id: BlockId,
    override val shape: Shape,
    val children: Set<SimpleBlock>
) : Block() {
    override fun withId(newId: BlockId): Block = ComplexBlock(newId, shape, children)
}


data class Canvas(
    val blockId: Int,
    val blocks: PersistentMap<BlockId, Block>
) {
    fun apply(move: Move): Canvas = when (move) {
        is LineCutMove -> TODO()
        is ColorMove -> TODO()
        is MergeMove -> TODO()
        is PointCutMove -> pointCut(move)
        is SwapMove -> swap(move)
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
            Shape(block.shape.lowerLeft, point),
        )
        val bottomRightBlock = bottomRightBuilder(
            ComplexId(blockId, 1),
            Shape(
                Point(point.x, block.shape.lowerLeft.y),
                Point(block.shape.upperRight.x, point.y)
            )
        )
        val topRightBlock = topRightBuilder(
            ComplexId(blockId, 2),
            Shape(point, block.shape.upperRight)
        )
        val topLeftBlock = topLeftBuilder(
            ComplexId(blockId, 3),
            Shape(
                Point(block.shape.lowerLeft.x, point.y),
                Point(point.x, block.shape.upperRight.y)
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
        return Canvas(this.blockId, newBlocks)
    }

    private fun pointCut(move: PointCutMove): Canvas {
        val blockId = move.block
        val block = blocks[blockId] ?: error("no block $blockId")
        val point = move.offset.also {
            check(it.isStrictlyInside(block.shape.lowerLeft, block.shape.upperRight)) {
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
                if (subBlock.shape.lowerLeft.x >= point.x && subBlock.shape.lowerLeft.y >= point.y) {
                    topRightBlocks.add(subBlock)
                    return@forEach
                }
                // Case 7
                if (subBlock.shape.upperRight.x <= point.x && subBlock.shape.upperRight.y <= point.y) {
                    bottomLeftBlocks.add(subBlock)
                    return@forEach
                }
                // Case 1
                if (subBlock.shape.upperRight.x <= point.x && subBlock.shape.lowerLeft.y >= point.y) {
                    topLeftBlocks.add(subBlock)
                    return@forEach
                }
                // Case 9
                if (subBlock.shape.lowerLeft.x >= point.x && subBlock.shape.upperRight.y <= point.y) {
                    bottomRightBlocks.add(subBlock)
                    return@forEach
                }
                // Case 5
                if (point.isInside(subBlock.shape.lowerLeft, subBlock.shape.upperRight)) {
                    bottomLeftBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 0), bottomLeftBlocks.size),
                            Shape(subBlock.shape.lowerLeft, point),
                            subBlock.color
                        )
                    )
                    bottomRightBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 1), bottomRightBlocks.size),
                            Shape(
                                Point(point.x, subBlock.shape.lowerLeft.y),
                                Point(subBlock.shape.upperRight.x, point.y)
                            ),
                            subBlock.color
                        )
                    )
                    topRightBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 2), topRightBlocks.size),
                            Shape(point, subBlock.shape.upperRight),
                            subBlock.color
                        )
                    )
                    topLeftBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 3), topLeftBlocks.size),
                            Shape(
                                Point(subBlock.shape.lowerLeft.x, point.y),
                                Point(point.x, subBlock.shape.upperRight.y)
                            ),
                            subBlock.color
                        )
                    )
                    return@forEach
                }

                // Case 2
                if (subBlock.shape.lowerLeft.x <= point.x
                    && point.x <= subBlock.shape.upperRight.x
                    && point.y < subBlock.shape.lowerLeft.y
                ) {
                    topLeftBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 4), topLeftBlocks.size),
                            Shape(
                                subBlock.shape.lowerLeft,
                                Point(point.x, subBlock.shape.upperRight.y)
                            ),
                            subBlock.color
                        )
                    )
                    topRightBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 5), topRightBlocks.size),
                            Shape(
                                Point(point.x, subBlock.shape.lowerLeft.y),
                                subBlock.shape.upperRight
                            ),
                            subBlock.color
                        )
                    )
                    return@forEach
                }
                // Case 8
                if (subBlock.shape.lowerLeft.x <= point.x
                    && point.x <= subBlock.shape.upperRight.x
                    && point.y > subBlock.shape.upperRight.y
                ) {
                    bottomLeftBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 6), bottomLeftBlocks.size),
                            Shape(
                                subBlock.shape.lowerLeft,
                                Point(point.x, subBlock.shape.upperRight.y)
                            ),
                            subBlock.color
                        )
                    )
                    bottomRightBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 7), bottomRightBlocks.size),
                            Shape(
                                Point(point.x, subBlock.shape.lowerLeft.y),
                                subBlock.shape.upperRight
                            ),
                            subBlock.color
                        )
                    )
                    return@forEach
                }
                // Case 4
                if (subBlock.shape.lowerLeft.y <= point.y
                    && point.y <= subBlock.shape.upperRight.y
                    && point.x < subBlock.shape.lowerLeft.x
                ) {
                    bottomRightBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 8), bottomRightBlocks.size),
                            Shape(
                                subBlock.shape.lowerLeft,
                                Point(subBlock.shape.upperRight.x, point.y)
                            ),
                            subBlock.color
                        )
                    )
                    topRightBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 9), topRightBlocks.size),
                            Shape(
                                Point(subBlock.shape.lowerLeft.x, point.y),
                                subBlock.shape.upperRight
                            ),
                            subBlock.color
                        )
                    )
                    return@forEach
                }
                // Case 6
                if (subBlock.shape.lowerLeft.y <= point.y
                    && point.y <= subBlock.shape.upperRight.y
                    && point.x > subBlock.shape.upperRight.x
                ) {
                    bottomLeftBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 10), bottomLeftBlocks.size),
                            Shape(
                                subBlock.shape.lowerLeft,
                                Point(subBlock.shape.upperRight.x, point.y)
                            ),
                            subBlock.color
                        )
                    )
                    topLeftBlocks.add(
                        SimpleBlock(
                            ComplexId(ComplexId(blockId, 11), topLeftBlocks.size),
                            Shape(
                                Point(subBlock.shape.lowerLeft.x, point.y),
                                subBlock.shape.upperRight
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
        val newBlock1 = block1.withId(block2.id)
        val newBlock2 = block2.withId(block1.id)
        val newBlocks = blocks.putAll(mapOf(newBlock1.id to newBlock1, newBlock2.id to newBlock2))
        return Canvas(blockId, newBlocks)
    }
}

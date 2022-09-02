package ru.spbstu.icfpc2022.canvas

import kotlinx.collections.immutable.PersistentMap
import ru.spbstu.icfpc2022.move.*


data class Point(
    val x: Int,
    val y: Int
) {
    override fun toString(): String = "[$x, $y]"
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
}

data class SimpleBlock(
    override val id: BlockId,
    override val shape: Shape,
    val color: Color
) : Block()

data class ComplexBlock(
    override val id: BlockId,
    override val shape: Shape,
    val children: Set<SimpleBlock>
) : Block()


data class Canvas(
    val blockId: Int,
    val shape: Shape,
    val blocks: PersistentMap<BlockId, Block>
) {
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
            Canvas(blockId, shape, blocks.put(block.id, newBlock))
        }

        is MergeMove -> {
            val first = blocks[move.first]!!
            val second = blocks[move.second]!!

            val lowerX = minOf(first.shape.lowerLeft.x, second.shape.lowerLeft.x)
            val lowerY = minOf(first.shape.lowerLeft.y, second.shape.lowerLeft.y)
            val upperX = maxOf(first.shape.upperRight.x, second.shape.upperRight.x)
            val upperY = minOf(first.shape.upperRight.y, second.shape.upperRight.y)

            val children = mutableSetOf<SimpleBlock>()
            when (first) {
                is SimpleBlock -> children += first
                is ComplexBlock -> children.addAll(first.children)
            }
            when (second) {
                is SimpleBlock -> children += second
                is ComplexBlock -> children.addAll(second.children)
            }

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
                shape,
                newBlocks.build()
            )
        }

        is PointCutMove -> TODO()
        is SwapMove -> TODO()
    }

    private fun verticalCut(block: Block, offset: Int): Canvas = when (block) {
        is SimpleBlock -> {
            val leftBlock = SimpleBlock(
                block.id + 0,
                Shape(block.shape.lowerLeft, Point(offset, block.shape.upperRight.y)),
                block.color
            )
            val rightBlock = SimpleBlock(
                block.id + 1,
                Shape(Point(offset, block.shape.lowerLeft.y), block.shape.upperRight),
                block.color
            )

            val newBlocks = blocks.builder()
            newBlocks.remove(block.id)
            newBlocks[leftBlock.id] = leftBlock
            newBlocks[rightBlock.id] = rightBlock

            Canvas(blockId, shape, newBlocks.build())
        }

        is ComplexBlock -> {
            val leftBlocks = mutableSetOf<SimpleBlock>()
            val rightBlocks = mutableSetOf<SimpleBlock>()
            for (child in block.children) {
                when {
                    child.shape.lowerLeft.x >= offset -> {
                        rightBlocks += child
                    }

                    child.shape.upperRight.x <= offset -> {
                        leftBlocks += child
                    }

                    else -> {
                        leftBlocks += SimpleBlock(
                            child.id + 0,
                            Shape(child.shape.lowerLeft, Point(offset, child.shape.upperRight.y)),
                            child.color
                        )
                        rightBlocks += SimpleBlock(
                            child.id + 1,
                            Shape(Point(offset, child.shape.lowerLeft.y), child.shape.upperRight),
                            child.color
                        )
                    }
                }
            }
            val leftBlock = ComplexBlock(
                block.id + 0,
                Shape(block.shape.lowerLeft, Point(offset, block.shape.upperRight.y)),
                leftBlocks
            )
            val rightBlock = ComplexBlock(
                block.id + 1,
                Shape(Point(offset, block.shape.lowerLeft.y), block.shape.upperRight),
                rightBlocks
            )

            val newBlocks = blocks.builder()
            newBlocks.remove(block.id)
            newBlocks[leftBlock.id] = leftBlock
            newBlocks[rightBlock.id] = rightBlock
            Canvas(blockId, shape, newBlocks.build())
        }
    }


    private fun horizontalCut(block: Block, offset: Int): Canvas = when (block) {
        is SimpleBlock -> {
            val leftBlock = SimpleBlock(
                block.id + 0,
                Shape(block.shape.lowerLeft, Point(block.shape.upperRight.x, offset)),
                block.color
            )
            val rightBlock = SimpleBlock(
                block.id + 1,
                Shape(Point(block.shape.lowerLeft.x, offset), block.shape.upperRight),
                block.color
            )

            val newBlocks = blocks.builder()
            newBlocks.remove(block.id)
            newBlocks[leftBlock.id] = leftBlock
            newBlocks[rightBlock.id] = rightBlock

            Canvas(blockId, shape, newBlocks.build())
        }

        is ComplexBlock -> {
            val bottomBlocks = mutableSetOf<SimpleBlock>()
            val topBlocks = mutableSetOf<SimpleBlock>()
            for (child in block.children) {
                when {
                    child.shape.lowerLeft.y >= offset -> {
                        topBlocks += child
                    }

                    child.shape.upperRight.y <= offset -> {
                        bottomBlocks += child
                    }

                    else -> {
                        bottomBlocks += SimpleBlock(
                            child.id + 0,
                            Shape(child.shape.lowerLeft, Point(child.shape.upperRight.x, offset)),
                            child.color
                        )
                        topBlocks += SimpleBlock(
                            child.id + 1,
                            Shape(Point(child.shape.lowerLeft.x, offset), child.shape.upperRight),
                            child.color
                        )
                    }
                }
            }
            val bottomBlock = ComplexBlock(
                block.id + 0,
                Shape(block.shape.lowerLeft, Point(block.shape.upperRight.x, offset)),
                bottomBlocks
            )
            val topBlock = ComplexBlock(
                block.id + 1,
                Shape(Point(block.shape.lowerLeft.x, offset), block.shape.upperRight),
                topBlocks
            )

            val newBlocks = blocks.builder()
            newBlocks.remove(block.id)
            newBlocks[bottomBlock.id] = bottomBlock
            newBlocks[topBlock.id] = topBlock
            Canvas(blockId, shape, newBlocks.build())
        }
    }
}
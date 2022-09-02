package ru.spbstu.icfpc2022.canvas

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
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
}

data class SimpleBlock(
    override val id: BlockId,
    override val shape: Shape,
    val color: Color
) : Block()

data class ComplexBlock(
    override val id: BlockId,
    override val shape: Shape,
    val children: Set<Block>
) : Block()


data class Canvas(
    val blockId: Int,
    val blocks: PersistentMap<BlockId, Block>
) {
    fun apply(move: Move): Canvas = when (move) {
        is LineCutMove -> TODO()
        is ColorMove -> TODO()
        is MergeMove -> TODO()
        is PointCutMove -> TODO()
        is SwapMove -> TODO()
    }
}
package ru.spbstu.icfpc2022.canvas

import kotlinx.collections.immutable.PersistentSet


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
    val points: List<Point>
)

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
}

data class SimpleBlock(
    override val id: BlockId,
    val shape: Shape,
    val color: Color
) : Block()

data class ComplexBlock(
    override val id: BlockId,
    val children: Set<Block>
) : Block()


data class Canvas(
    val blockId: Int,
    val blocks: PersistentSet<Block>
)
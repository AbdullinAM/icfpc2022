package ru.spbstu.icfpc2022.move

import ru.spbstu.icfpc2022.canvas.BlockId
import ru.spbstu.icfpc2022.canvas.Color
import ru.spbstu.icfpc2022.canvas.Point

sealed class Move {
    abstract val cost: Long
}

enum class Orientation {
    X, Y
}

data class LineCutMove(
    val block: BlockId,
    val orientation: Orientation,
    val offset: Int
) : Move() {
    override val cost: Long = 7L
    override fun toString(): String = "cut [$block] [$orientation] [$offset]"
}

data class PointCutMove(
    val block: BlockId,
    val offset: Point
) : Move() {
    override val cost: Long = 10L
    override fun toString(): String = "cut [$block] $offset"
}

data class ColorMove(
    val block: BlockId,
    val color: Color
) : Move() {
    override val cost: Long = 5L
    override fun toString(): String = "color [$block] $color"
}

data class SwapMove(
    val first: BlockId,
    val second: BlockId
) : Move() {
    override val cost: Long = 3L
    override fun toString(): String = "swap [$first] [$second]"
}

data class MergeMove(
    val first: BlockId,
    val second: BlockId
) : Move() {
    override val cost: Long = 1L
    override fun toString(): String = "merge [$first] [$second]"
}
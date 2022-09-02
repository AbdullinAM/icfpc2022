package ru.spbstu.icfpc2022.robovinchi

import com.sksamuel.scrimage.ImmutableImage
import kotlinx.collections.immutable.PersistentMap
import ru.spbstu.icfpc2022.canvas.Block
import ru.spbstu.icfpc2022.canvas.BlockId
import ru.spbstu.icfpc2022.canvas.Canvas
import ru.spbstu.icfpc2022.canvas.SimpleBlock
import ru.spbstu.icfpc2022.move.Move

object StateCollector {

    val canvasHeight = 400.0
    val canvasWidth = 400.0
    var pathToProblemImage = ""
    val commandToCanvas = mutableListOf<Pair<Move, List<SimpleBlock>>>()

}
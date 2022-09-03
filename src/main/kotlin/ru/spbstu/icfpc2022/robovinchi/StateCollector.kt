package ru.spbstu.icfpc2022.robovinchi

import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.canvas.SimpleBlock
import ru.spbstu.icfpc2022.move.Move

object StateCollector {

    var turnMeOff: Boolean = false

    lateinit var task: Task
    val canvasHeight = 400.0
    val canvasWidth = 400.0
    var pathToProblemImage = ""
    val commandToCanvas = mutableListOf<Pair<Move, List<SimpleBlock>>>()

}

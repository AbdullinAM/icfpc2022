package ru.spbstu.icfpc2022.algo

//import ru.spbstu.icfpc2022.canvas.BlockId
//import ru.spbstu.icfpc2022.canvas.Color
//import ru.spbstu.icfpc2022.canvas.Point
//import ru.spbstu.icfpc2022.imageParser.color
//import ru.spbstu.icfpc2022.imageParser.get
//import ru.spbstu.icfpc2022.move.Move
//import ru.spbstu.icfpc2022.move.Orientation
//import ru.spbstu.icfpc2022.move.PointCutMove
//
//class RandomBoxCutter(val maxColors: Int): Solver(task) {
//    var state: PersistentState = PersistentState(task)
//
//    fun PersistentState.blockColors(blockId: BlockId): Set<Color> {
//        state.task.targetImage.autocrop()
//        val block = canvas.blocks[blockId]!!
//
//        val subImage = task.targetImage.subimage(block.shape.lowerLeft.x, block.shape.lowerLeft.y, block.shape.width, block.shape.height)
//        return subImage.iterator().asSequence().mapTo(mutableSetOf()) { it.color }
//    }
//
//    fun cutBlock(blockId: BlockId): PersistentState {
//        val block = state.canvas.blocks[blockId]!!
//
//        val possiblePoints = (2 until (block.shape.width - 2)).flatMap { x ->
//            (2 until (block.shape.height - 2)).map { y -> PointCutMove(blockId, Point(x, y))}
//        }
//        val states = possiblePoints.map { state.move(it) }
//        val cutResult = listOf(0,1,2,3).map { blockId + it }
//
//        val colors = states.groupBy { state -> cutResult.map { state.blockColors(it) }.sumOf { it.size } }
//
//        colors[colors.keys.min()]?.maxBy {  }
//    }
//
//    override fun solve(): List<Move> {
//
//    }
//
//
//}

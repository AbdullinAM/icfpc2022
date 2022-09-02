package ru.spbstu.icfpc2022.algo

import com.sksamuel.scrimage.ImmutableImage
import ru.spbstu.icfpc2022.canvas.BlockId
import ru.spbstu.icfpc2022.canvas.Canvas
import ru.spbstu.icfpc2022.move.Move

class DummyBlockAverager(
    task: ImmutableImage
) : Solver(task) {


    override fun solve(): List<Move> {
        var state = PersistentState(
            task,
            Canvas(task.width, task.height)
        )

        val queue = ArrayDeque<BlockId>()
        queue.addAll(state.canvas.allSimpleBlocks().map { it.id })
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            val currentBlock = state.canvas.blocks[current]!!
            val middlePoint = current
//            val cut = Poi
        }

        TODO()
    }
}
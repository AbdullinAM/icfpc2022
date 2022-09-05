package ru.spbstu.icfpc2022

import ru.spbstu.icfpc2022.algo.*
import ru.spbstu.icfpc2022.canvas.*
import ru.spbstu.icfpc2022.imageParser.parseImage
import ru.spbstu.icfpc2022.move.*
import ru.spbstu.icfpc2022.robovinchi.Robovinchi
import ru.spbstu.icfpc2022.robovinchi.StateCollector
import tornadofx.launch

fun main() = try {
    val problemId = 1
//    val problems = getProblems()
//    val submissions = submissions()
//    val bestSubmissions = submissions.bestSubmissions()

//    val problem = problems.first { it.id == problemId }
    val problem = Problem(problemId, "huy", "huy", InitialConfig.default(), null, parseImage("problems/$problemId.png"))
    StateCollector.pathToProblemImage = "problems/$problemId.png"
    val im = problem.target
    val bestScore = 16818L//bestSubmissions[problem.id]?.score
    var task = Task(problem.id, im, problem.initialConfig, bestScore)
    StateCollector.task = task
    var commands = with(MutableState(task.initialState.withIncrementalSimilarity())) {
        val xSnaps = task.snapPoints.pointData.entries
            .map { it.key to it.value.size }
            .sortedByDescending { it.second }
        val ySnaps = task.snapPoints.pointData.entries
            .flatMap { xdata ->
                xdata.value.entries.map { ydata -> xdata.key to ydata.key }
            }
            .groupBy { it.second }
            .mapValues { it.value.size }
            .map { it.toPair() }
            .sortedByDescending { it.second }
        state = state.move(ColorMove(SimpleId(0), Color(0, 74, 173, 255)))
        //                w1 | w2 | w3 | w4 | w5 | b1 | b2 | b3 | b4 | cb2| hlam
        //                40 | 40 | 40 | 39 | 40 | 39 | 40 | 40 | 39 | 40 | 3
        val vSplits = listOf(40, 80, 120, 159, 199, 238, 278, 318, 357)
        //          hlam | h0 | h1 | h2 | h3 | h4 | h5 | h6 | h7 | h8 | hlam
        //            41 | 40 | 40 | 40 | 40 | 39 | 40 | 39 | 40 | 40 | 1
        val hSplits = listOf(81, 121, 161, 201, 240, 280, 319, 359)

        val (workArea, hlam) = move { lineCut(SimpleId(0), Orientation.X, 397) }
        val (workArea1, hlam1) = move { lineCut(workArea.id, Orientation.Y, 399) }
        val cuttedBlocks = move { pointCut(workArea1.id, Point(vSplits[8], 41)) }
        val (white, black) = move { lineCut(cuttedBlocks.last().id, Orientation.X, vSplits[4]) }
        state = state.move(ColorMove(white.id, Color(255, 255, 255, 255)))
        state = state.move(ColorMove(black.id, Color(0, 0, 0, 255)))
        val (wleft1, white5) = move { lineCut(white.id, Orientation.X, vSplits[3]) }
        val (wleft2, wright2) = move { lineCut(wleft1.id, Orientation.X, vSplits[1]) }
        val (white1, white2) = move { lineCut(wleft2.id, Orientation.X, vSplits[0]) }
        val (white3, white4) = move { lineCut(wright2.id, Orientation.X, vSplits[2]) }
        val (bleft, bright) = move { lineCut(black.id, Orientation.X, vSplits[6]) }
        val (black1, black2) = move { lineCut(bleft.id, Orientation.X, vSplits[5]) }
        val (black3, black4) = move { lineCut(bright.id, Orientation.X, vSplits[7]) }
        state = state.move(SwapMove(white2.id, black2.id))
        state = state.move(SwapMove(white4.id, black4.id))
        val lines = listOf(white1, black2, white3, black4, white5, black1, white2, black3, white4)
        var mergedLines = lines.reduce { res, block -> move { merge(res.id, block.id) } }
        val hLines = arrayListOf<Block>()
        for (split in hSplits) {
            val (line, other) = move { lineCut(mergedLines.id, Orientation.Y, split) }
            mergedLines = other
            hLines += line
        }
        hLines += mergedLines

//        val (blkSource, tmp) = move { lineCut(cuttedBlocks[2].id, Orientation.X, 397) }
        val blkSource = cuttedBlocks[2]
        val (x0, other) = move { lineCut(hLines[3].id, Orientation.X, vSplits[0]) }
        val (smallWithTail, upTail) = move { lineCut(blkSource.id, Orientation.Y, hSplits[3]) }
        val (downTail, smallBlock) = move { lineCut(smallWithTail.id, Orientation.Y, hSplits[2]) }
        val lineWithSmall = move { merge(other.id, smallBlock.id) }
        state = state.move(SwapMove(hLines[1].id, lineWithSmall.id))


        val (x1, other1) = move { lineCut(hLines[7].id, Orientation.X, vSplits[0]) }
        val (smallWithTail1, upTail1) = move { lineCut(upTail.id, Orientation.Y, hSplits[7]) }
        val (downTail1, smallBlock1) = move { lineCut(smallWithTail1.id, Orientation.Y, hSplits[6]) }
        val lineWithSmall1 = move { merge(other1.id, smallBlock1.id) }
        state = state.move(SwapMove(hLines[5].id, lineWithSmall1.id))

        run {
            val (blk1, hueta) = move { lineCut(hLines[5].id, Orientation.X, vSplits[8]) }
            state = state.move(ColorMove(hueta.id, Color(0, 74, 173, 255)))
        }
        run {
            val (blk2, huet2) = move { lineCut(hLines[1].id, Orientation.X, vSplits[8]) }
            state = state.move(ColorMove(huet2.id, Color(0, 74, 173, 255)))
        }
        run {
//            val (blk1, hueta) = move { lineCut(x1.id, Orientation.X, vSplits[0]) }
            state = state.move(ColorMove(x1.id, Color(0, 0, 0, 255)))
        }
        run {
//            val (blk1, hueta) = move { lineCut(hLines[5].id, Orientation.X, vSplits[0]) }
            state = state.move(ColorMove(x0.id, Color(0, 0, 0, 255)))
        }
        run {
            val (blk1, hueta) = move { lineCut(lineWithSmall1.id, Orientation.X, vSplits[7]) }
            state = state.move(ColorMove(hueta.id, Color(0, 0, 0, 255)))
        }
        run {
            val (blk1, hueta) = move { lineCut(lineWithSmall.id, Orientation.X, vSplits[7]) }
            state = state.move(ColorMove(hueta.id, Color(0, 0, 0, 255)))
        }
        run {
            val (blk1, hueta) = move { lineCut(hLines[0].id, Orientation.X, vSplits[7]) }
            state = state.move(ColorMove(hueta.id, Color(0, 74, 173, 255)))
        }
        println(state.cost)
        println(state.score)
        state
    }

    if (commands.score < task.bestScoreOrMax) {
        submit(problem.id, commands.commands.joinToString("\n"))
    }

    println("Solution size: ${commands.commands.size}")
    launch<Robovinchi>()
} finally {
    shutdownClient()
}

private class MutableState(initState: PersistentState) {
    var state = initState
    fun <T> move(action: PersistentState.() -> Pair<PersistentState, T>): T {
        val (newState, res) = state.action()
        state = newState
        return res
    }
}

private fun PersistentState.lineCut(
    block: BlockId,
    orientation: Orientation,
    offset: Int
): Pair<PersistentState, List<Block>> {
    val blocksBefore = canvas.blocks.keys
    val newState = move(LineCutMove(block, orientation, offset))
    val newBlocks = (newState.canvas.blocks.keys - blocksBefore).map { newState.canvas.blocks[it]!! }
    return newState to newBlocks
}

private fun PersistentState.pointCut(
    block: BlockId,
    point: Point
): Pair<PersistentState, List<Block>> {
    val blocksBefore = canvas.blocks.keys
    val newState = move(PointCutMove(block, point))
    val newBlocks = (newState.canvas.blocks.keys - blocksBefore).map { newState.canvas.blocks[it]!! }
    return newState to newBlocks
}

private fun PersistentState.merge(
    a: BlockId,
    b: BlockId
): Pair<PersistentState, Block> {
    val blocksBefore = canvas.blocks.keys
    val newState = move(MergeMove(a, b))
    val newBlocks = (newState.canvas.blocks.keys - blocksBefore).map { newState.canvas.blocks[it]!! }
    return newState to newBlocks.single()
}

package ru.spbstu.icfpc2022.algo

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.sksamuel.scrimage.nio.PngWriter
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.spbstu.icfpc2022.algo.tactics.computeBlockAverage
import ru.spbstu.icfpc2022.algo.tactics.computeBlockMedian
import ru.spbstu.icfpc2022.canvas.*
import ru.spbstu.icfpc2022.imageParser.toImage
import ru.spbstu.icfpc2022.move.*
import java.io.File
import java.lang.reflect.Type
import kotlin.random.Random
import kotlin.system.exitProcess

class StupidFuzzer(task: Task) : Solver(task) {

    val mutex = Mutex()
    override fun solve(): PersistentState {
        runBlocking(newFixedThreadPoolContext(16, "hui")) {
            try {
                withTimeout(3_000_000) {
                    val l = (0..1000).map { launch { StupidFuzzer(task).sol(0) } }
                    l.forEach { it.join() }
                    return@withTimeout
                }
                println("ITERATION 2")
                if (BestStatesCollector.curBestState == null) exitProcess(0)
                withTimeout(3_000_000) {
                    val l = (0..100).map { launch { StupidFuzzer(task).sol(1) } }
                    l.forEach { it.join() }
                    return@withTimeout
                }
                exitProcess(0)
            } catch (e: Throwable) {
                println("E = $e")
            }
        }
        return PersistentState(task)
    }

    private suspend fun sol(iteration: Int): PersistentState {
        val initState =
            BestStatesCollector.curBestState ?: PersistentState(
                task,
                Canvas.empty(task.targetImage.width, task.targetImage.height)
            )
        val queue = ArrayDeque<PersistentState>()
        queue.add(initState)

        val coloredBlocks = mutableSetOf<BlockId>()
        var currentState = queue.removeFirst()
        var randomBlockId = currentState.canvas.blocks.keys.filter { it !in coloredBlocks }.random()
        var randomBlock = currentState.canvas.blocks[randomBlockId]!!
        var color = computeBlockAverage(currentState.task.targetImage, randomBlock.shape)
        var move = ColorMove(randomBlockId, color)
        var taskBestScore = if (iteration == 0) task.bestScore!! else BestStatesCollector.bestScore
        if (iteration == 0) {
            val coloredInitState = currentState.move(move)
            if (currentState.score > coloredInitState.score) {
                currentState = coloredInitState
            }
        }
        var currentStateScore = currentState.score
        var ind = 0
        while (true) {
            ind++
            if (ind % 100 == 0) println("CUR IND = $ind")
            yield()
            while (true) {
                randomBlockId = currentState.canvas.blocks.keys.random()
                randomBlock = currentState.canvas.blocks[randomBlockId]!!

                if (randomBlock.shape.width <= 1 || randomBlock.shape.height <= 1) {
                    continue
                }
                break
            }
            val cutMove = if (Random.nextBoolean()) {
                val randomOrientation = Orientation.values().random()

                val randomXCoor = Random.nextInt(randomBlock.shape.lowerLeft.x, randomBlock.shape.lowerRight.x - 1)
                val randomYCoor = Random.nextInt(randomBlock.shape.lowerLeft.y, randomBlock.shape.upperLeft.y)
                val randomSnap = task.closestSnap(Point(randomXCoor, randomYCoor), randomBlock.shape) ?: continue
                val randomOffset = when (randomOrientation) {
                    Orientation.X -> randomSnap.x
                    Orientation.Y -> randomSnap.y
                }
                LineCutMove(randomBlockId, randomOrientation, randomOffset)
            } else {
                val randomXCoor = Random.nextInt(randomBlock.shape.lowerLeft.x, randomBlock.shape.lowerRight.x - 1)
                val randomYCoor = Random.nextInt(randomBlock.shape.lowerLeft.y, randomBlock.shape.upperLeft.y)
                val randomSnap = task.closestSnap(Point(randomXCoor, randomYCoor), randomBlock.shape) ?: continue
                PointCutMove(randomBlockId, randomSnap)
            }
            val oldStateBlocks = currentState.canvas.blocks.keys
            val newState = currentState.move(cutMove)
            val newStateBlocks = newState.canvas.blocks
            val addedBlocks = newStateBlocks.keys.filter { it !in oldStateBlocks }
            randomBlockId = addedBlocks.randomOrNull() ?: continue
            randomBlock = newState.canvas.blocks[randomBlockId]!!
            color = if (Random.nextBoolean()) {
                computeBlockAverage(newState.task.targetImage, randomBlock.shape)
            } else {
                computeBlockMedian(newState.task.targetImage, randomBlock.shape)
            }
            move = ColorMove(randomBlockId, color)
            val coloredState = newState.move(move)
            val coloredStateScore = coloredState.score
            //println("NEW STATE SCORE = $coloredStateScore")
            if (iteration == 0) {
                if (ind > 30 && currentStateScore.toDouble() / taskBestScore > 1.6) return currentState
                if (ind > 50 && currentStateScore.toDouble() / taskBestScore > 1.4) return currentState
                if (ind > 100 && currentStateScore.toDouble() / taskBestScore > 1.2) return currentState
                if (ind > 250 && currentStateScore.toDouble() / taskBestScore > 1.1) return currentState
            }
            if (iteration == 1) {
                if (ind > 30 && currentStateScore.toDouble() / taskBestScore > 1.15) return currentState
                if (ind > 50 && currentStateScore.toDouble() / taskBestScore > 1.07) return currentState
                if (ind > 100 && currentStateScore.toDouble() / taskBestScore > 1.05) return currentState
                //if (ind == 250 && coloredStateScore.toDouble() / taskBestScore > 1.0) return currentState
            }
            //if (ind == 300 && coloredStateScore.toDouble() / task.bestScore!!.toDouble() > 1.05) return currentState
            //if (ind == 400 && coloredStateScore.toDouble() / task.bestScore!!.toDouble() > 1.1) return currentState
            if (ind > 2000 && currentStateScore == BestStatesCollector.bestScore) {
                println("DUMPING BECAUSE OF INDEX $ind")
                mutex.withLock {
                    BestStatesCollector.curBestState = currentState
                }
                dump(currentStateScore, taskBestScore, currentState)
                return currentState
            }
            if (ind > 2000) {
                return currentState
            }
            if (coloredStateScore < currentStateScore) {
                if (coloredStateScore < BestStatesCollector.bestScore) {
                    mutex.withLock {
                        BestStatesCollector.bestScore = coloredStateScore
                    }
                    println("BEST SCORE UPDATED to $coloredStateScore")
                }
                println("I AM IMPROVED newScore = $coloredStateScore ind = $ind ${coloredStateScore.toDouble() / task.bestScore!!.toDouble()}")
                currentState = coloredState
                currentStateScore = currentState.score
            }
            if (currentStateScore < taskBestScore && currentStateScore <= BestStatesCollector.bestScore) {
                println("DUMPING BETTER SOLUTION")
                dump(coloredStateScore, taskBestScore, currentState)
                mutex.withLock {
                    BestStatesCollector.curBestState = currentState
                }
                taskBestScore = currentStateScore
            }
        }

        return initState
    }


    private fun dump(coloredStateScore: Long, bestScore: Long, currentState: PersistentState) {
        val randomInt = Random.nextInt(0, 1000)
        println("DUMPING SOLUTION to $randomInt ${coloredStateScore.toDouble() / bestScore}")
        currentState.canvas.toImage().flipY().forWriter(PngWriter(0))
            .write(File("solutions/${task.problemId}_$randomInt.png"))
        val commands = currentState.dumpSolution()
        File("solutions/${task.problemId}_$randomInt.txt").writeText(commands)
        val gson = Gson()
        val json = gson.toJson(currentState.canvas)
        File("solutions/${task.problemId}_$randomInt.json").writeText(json)
        File("solutions/${task.problemId}_${randomInt}_${coloredStateScore.toDouble() / bestScore}_${currentState.score}.txt").writeText(
            ""
        )
    }

}

class BlockIdCreator() : JsonDeserializer<BlockId> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): BlockId {
        return SimpleId(0)
    }
}

class BlockCreator() : JsonDeserializer<Block> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Block {
        println()
        TODO()
    }
}

object BestStatesCollector {
    var curBestState: PersistentState? = null
    var bestScore: Long = Long.MAX_VALUE
}
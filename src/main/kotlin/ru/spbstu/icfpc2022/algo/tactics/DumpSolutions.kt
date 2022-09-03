package ru.spbstu.icfpc2022.algo.tactics

import com.sksamuel.scrimage.nio.PngWriter
import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.imageParser.toImage
import java.io.File

class DumpSolutions(task: Task, tacticStorage: TacticStorage): Tactic(task, tacticStorage) {
    override fun invoke(state: PersistentState): PersistentState {
        File("solutions/").mkdirs()
        state.canvas.toImage().flipY().forWriter(PngWriter(0)).write(File("solutions/${task.problemId}.png"))
        println("Task#${task.problemId}")
        val score = state.score
        val isBetter = task.bestScore == null || task.bestScore > score
        println("$score | ${if (isBetter) "better" else "worse"} | current ${task.bestScore}")
        return state
    }

}

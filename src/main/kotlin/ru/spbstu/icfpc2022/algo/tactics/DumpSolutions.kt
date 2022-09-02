package ru.spbstu.icfpc2022.algo.tactics

import com.sksamuel.scrimage.nio.PngWriter
import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.imageParser.toImage
import java.io.File

class DumpSolutions(task: Task, tacticStorage: TacticStorage): Tactic(task, tacticStorage) {
    override fun invoke(state: PersistentState): PersistentState {
        File("solutions/").mkdirs()
        state.canvas.toImage().forWriter(PngWriter(0)).write(File("solutions/${task.problemId}.png"))
        println(state.score)
        return state
    }

}

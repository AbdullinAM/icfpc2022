import com.sksamuel.scrimage.nio.PngWriter
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.getProblems
import ru.spbstu.icfpc2022.imageParser.score
import ru.spbstu.icfpc2022.imageParser.toImage
import ru.spbstu.icfpc2022.shutdownClient
import java.io.File

fun main() = try {
    val problems = getProblems()
    val problemsWithCanvas = problems.filter { it.initialCanvas != null }
    problemsWithCanvas.forEach { problem ->
        val task = Task(problem.id, problem.target, problem.initialConfig, bestScore = null)
        println(score(task.initialCanvas, problem.initialCanvas!!))
        task.initialCanvas.toImage().flipY().forWriter(PngWriter(0)).write(File("solutions/${task.problemId}_initial_canvas.png"))
        problem.initialCanvas!!.flipY().forWriter(PngWriter(0)).write(File("solutions/${task.problemId}_initial_canvas_target.png"))
    }

} finally {
    shutdownClient()
}
package ru.spbstu.icfpc2022

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sksamuel.scrimage.ImmutableImage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.spbstu.icfpc2022.algo.*
import ru.spbstu.icfpc2022.algo.tactics.*
import ru.spbstu.icfpc2022.canvas.Color
import ru.spbstu.icfpc2022.canvas.Point
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.icfpc2022.canvas.SimpleBlock
import ru.spbstu.icfpc2022.canvas.SimpleId
import ru.spbstu.icfpc2022.imageParser.parseImage
import java.net.URL

private const val token =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6Im5hcHN0ZXJfMTk5N0BtYWlsLnJ1IiwiZXhwIjoxNjYyNDQyNTY1LCJvcmlnX2lhdCI6MTY2MjM1NjE2NX0.X52vfYqjxJ6jnTC81F_BWcrZdU1iUNmDSAd9QTxys7E"

private const val baseUrl = "https://robovinci.xyz/api"

private val client = OkHttpClient()

class RawInitialConfigBlock(
    val blockId: Int,
    val bottomLeft: IntArray,
    val topRight: IntArray,
    val color: IntArray?,
    val pngBottomLeftPoint: IntArray?
) {
    fun resolveSimpleBlock(): SimpleBlock {
        val newColor = color ?: intArrayOf(255, 255, 0, 0)
        val bl = Point(bottomLeft[0], bottomLeft[1])
        val ur = Point(topRight[0], topRight[1])
        val c = Color(newColor[0], newColor[1], newColor[2], newColor[3])
        return SimpleBlock(SimpleId(blockId), Shape(bl, ur), c)
    }
}

data class InitialConfig(
    val width: Int,
    val height: Int,
    val initialPng: ImmutableImage?,
    val blocks: List<SimpleBlock>
) {
    companion object {
        fun default() = InitialConfig(
            400, 400, null, listOf(
                SimpleBlock(
                    SimpleId(0),
                    Shape(Point(0, 0), Point(400, 400)),
                    Color(255, 255, 255, 255)
                )
            )
        )
    }
}

class RawInitialConfig(
    val width: Int,
    val height: Int,
    val sourcePngJSON: String?,
    val sourcePngPNG: String?,
    val blocks: Array<RawInitialConfigBlock>
) {
    fun resolve() = InitialConfig(width, height, sourcePngPNG?.let { parseImage(URL(it)) }, blocks.map { it.resolveSimpleBlock() })
}

data class Problem(
    val id: Int,
    val name: String,
    val description: String,
    val initialConfig: InitialConfig,
    val initialCanvas: ImmutableImage?,
    val target: ImmutableImage
)

class RawProblems(
    val problems: List<RawProblem>
)

class RawProblem(
    val id: Int,
    val name: String,
    val description: String,
    val canvas_link: String,
    val initial_config_link: String,
    val target_link: String
) {
    fun load(): Problem {
        val config = if (initial_config_link.isBlank()) {
            InitialConfig.default()
        } else {
            val mapper = jacksonObjectMapper()
            mapper.readValue(URL(initial_config_link), RawInitialConfig::class.java).resolve()
        }
        val initialCanvas = if (canvas_link.isBlank()) {
            null
        } else {
            parseImage(URL(canvas_link))
        }
        val target = parseImage(URL(target_link))
        return Problem(id, name, description, config, initialCanvas, target)
    }
}

class RawSubmissions(
    val submissions: List<Submission>
)

data class Submission(
    val id: Int,
    val problem_id: Int,
    val submitted_at: String,
    val status: String,
    val score: Long,
    val error: String
)

fun getProblems(): List<Problem> {
    val request = Request.Builder()
        .url("$baseUrl/problems")
        .addHeader("Authorization", "Bearer $token")
        .get()
        .build()
    val response = client.newCall(request).execute()
    val mapper = jacksonObjectMapper()
    val rawProblems = mapper.readValue(response.body!!.string(), RawProblems::class.java)
    return rawProblems.problems.map { it.load() }
}

fun submissions(): List<Submission> {
    val request = Request.Builder()
        .url("$baseUrl/submissions")
        .addHeader("Authorization", "Bearer $token")
        .get()
        .build()
    val response = client.newCall(request).execute()
    val mapper = jacksonObjectMapper()
    return mapper.readValue(response.body!!.string(), RawSubmissions::class.java).submissions
}

fun submit(problemId: Int, code: String): String? {
    val payload = MultipartBody.Builder().setType(MultipartBody.FORM)
        .addFormDataPart("file", "submission.isl", code.toRequestBody("text/plain".toMediaType()))
        .build()
    val request = Request.Builder()
        .url("$baseUrl/submissions/${problemId}/create")
        .addHeader("Authorization", "Bearer $token")
        .post(payload)
        .build()
    return client.newCall(request).execute().body?.string()
}

fun List<Submission>.bestSubmissions() =
    groupBy { it.problem_id }
        .mapValues { (_, subs) -> subs.filter { it.status == "SUCCEEDED" } }
        .mapValues { (_, subs) -> subs.minByOrNull { it.score } }

fun shutdownClient() = client.connectionPool.evictAll()

fun main() = try {
    val problems = getProblems()
    val submissions = submissions()
    val bestSubmissions = submissions.bestSubmissions()

    for (problem in problems) {
        val im = problem.target

        val bestScore = bestSubmissions[problem.id]?.score
        val task = Task(problem.id, im, problem.initialConfig, bestScore = bestScore)
        val rectangleCropDummy = RectangleCropDummy(task)
        val solution = rectangleCropDummy.solve()
        val dumper = DumpSolutions(task, TacticStorage())
        dumper(solution)
        if (solution.score < (bestScore ?: Long.MAX_VALUE)) {
            submit(problem.id, solution.commands.joinToString("\n"))
        }
    }
} finally {
    shutdownClient()
}

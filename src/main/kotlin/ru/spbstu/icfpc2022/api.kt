package ru.spbstu.icfpc2022

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.spbstu.icfpc2022.algo.*
import ru.spbstu.icfpc2022.imageParser.parseImage
import java.net.URL
import kotlin.io.path.Path
import kotlin.io.path.outputStream

private const val token =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6Im5hcHN0ZXJfMTk5N0BtYWlsLnJ1IiwiZXhwIjoxNjYyMjkzMzY0LCJvcmlnX2lhdCI6MTY2MjIwNjk2NH0.McBW7n5aGifJUwOtkD_yhyR2VVN1BR6SPLkCfZNLBiM"

private const val baseUrl = "https://robovinci.xyz/api"

private val client = OkHttpClient()


data class Problem(
    val id: Int,
    val name: String,
    val description: String,
    val canvas_link: String,
    val initial_config_link: String,
    val target_link: String
)

data class Problems(val problems: List<Problem>)
data class Submission(
    val id: Int,
    val problem_id: Int,
    val submitted_at: String,
    val status: String,
    val score: Long,
    val error: String
)

data class Submissions(val submissions: List<Submission>)

fun downloadProblems(problems: Problems) {
    problems.problems.forEach {
        val request = Request.Builder().url(it.target_link).build()
        val response = client.newCall(request).execute().body!!
        Path("problems/${it.id}.png").outputStream().use { out ->
            out.write(response.bytes())
        }
    }
}

fun getProblems(): Problems {
    val request = Request.Builder()
        .url("$baseUrl/problems")
        .addHeader("Authorization", "Bearer $token")
        .get()
        .build()
    val response = client.newCall(request).execute()
    val mapper = jacksonObjectMapper()
    return mapper.readValue(response.body!!.string(), Problems::class.java)
}

fun submissions(): Submissions {
    val request = Request.Builder()
        .url("$baseUrl/submissions")
        .addHeader("Authorization", "Bearer $token")
        .get()
        .build()
    val response = client.newCall(request).execute()
    val mapper = jacksonObjectMapper()
    return mapper.readValue(response.body!!.string(), Submissions::class.java)
}

fun submit(problemId: Int, code: String) {
    val payload = MultipartBody.Builder().setType(MultipartBody.FORM)
        .addFormDataPart("file", "submission.isl", code.toRequestBody("text/plain".toMediaType()))
        .build()
    val request = Request.Builder()
        .url("$baseUrl/submissions/${problemId}/create")
        .addHeader("Authorization", "Bearer $token")
        .post(payload)
        .build()
    client.newCall(request).execute().also { println(it.body?.string()) }
}

fun Submissions.bestSubmissions() = submissions
    .groupBy { it.problem_id }
    .mapValues { (_, subs) -> subs.filter { it.status == "SUCCEEDED" } }
    .mapValues { (_, subs) -> subs.minByOrNull { it.score } }

fun shutdownClient() = client.connectionPool.evictAll()

fun main() = try {
    val problems = getProblems()
    val submissions = submissions()
    val bestSubmissions = submissions.bestSubmissions()

//    downloadProblems(problems)
    for (problem in problems.problems) {
        val im = parseImage(URL(problem.target_link))

        val bestScore = bestSubmissions[problem.id]?.score
        val task = Task(problem.id, im, bestScore = bestScore)
        val rectangleCropDummy = RectangleCropDummy(task)
        val solution = rectangleCropDummy.solve()
        if (solution.score < (bestScore ?: Long.MAX_VALUE)) {
            submit(problem.id, solution.commands.joinToString("\n"))
        }
    }
} finally {
    shutdownClient()
}

package ru.spbstu.icfpc2022

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.spbstu.icfpc2022.algo.AutocropDummy
import ru.spbstu.icfpc2022.algo.DummyBlockAverager
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.imageParser.parseImage
import java.net.URL
import kotlin.io.path.Path
import kotlin.io.path.outputStream

private const val token =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6Im5hcHN0ZXJfMTk5N0BtYWlsLnJ1IiwiZXhwIjoxNjYyMjA2NDg1LCJvcmlnX2lhdCI6MTY2MjEyMDA4NX0.lYQen_2LJelsyZ3czfO3jqTcLmyJM7GL7YBtvwoyJ8U"

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
    val score: Int
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

fun main() = try {
    val problems = getProblems()
    downloadProblems(problems)
    for (problem in problems.problems) {
        val im = parseImage(URL(problem.target_link))

        val task = Task(problem.id, im)
        val autocropDummy = AutocropDummy(task)
        val solution = autocropDummy.solve()
        submit(problem.id, solution.joinToString("\n"))
    }
} finally {
    client.connectionPool.evictAll()
}

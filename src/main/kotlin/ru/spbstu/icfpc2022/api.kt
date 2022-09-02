package ru.spbstu.icfpc2022

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.io.path.Path
import kotlin.io.path.outputStream

private const val token =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6Im5hcHN0ZXJfMTk5N0BtYWlsLnJ1IiwiZXhwIjoxNjYyMjA2NDg1LCJvcmlnX2lhdCI6MTY2MjEyMDA4NX0.lYQen_2LJelsyZ3czfO3jqTcLmyJM7GL7YBtvwoyJ8U"

private const val baseUrl = "https://robovinci.xyz/api"

data class Problem(
    val id: Int,
    val name: String,
    val description: String,
    val canvas_link: String,
    val initial_config_link: String,
    val target_link: String
)

data class Problems(val problems: List<Problem>)

fun main() {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("$baseUrl/problems")
        .addHeader("Authorization", "Bearer $token")
        .get()
        .build()
    val response = client.newCall(request).execute()
    val mapper = jacksonObjectMapper()
    val problems = mapper.readValue(response.body!!.string(), Problems::class.java)
    problems.problems.forEach {
        val request = Request.Builder().url(it.target_link).build()
        val response = client.newCall(request).execute().body!!
        Path("problems/${it.id}.png").outputStream().use { out ->
            out.write(response.bytes())
        }
    }
}

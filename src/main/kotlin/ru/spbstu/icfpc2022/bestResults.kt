package ru.spbstu.icfpc2022

fun main() = try {
    val problems = getProblems()
    val submissions = submissions()
    val bestSubmissions = submissions.bestSubmissions()

//    downloadProblems(problems)
    for (problem in problems.problems) {
        val bestScore = bestSubmissions[problem.id]?.score
        println("Task#${problem.id}: $bestScore")
    }
} finally {
    shutdownClient()
}

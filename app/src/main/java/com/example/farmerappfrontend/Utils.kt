package com.example.farmerappfrontend

import kotlin.math.min

fun levenshtein(a: String, b: String): Int {
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1] || areOcrSimilar(a[i - 1], b[j - 1])) 0 else 1
            dp[i][j] = min(
                min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                dp[i - 1][j - 1] + cost
            )
        }
    }
    return dp[a.length][b.length]
}
fun areOcrSimilar(a: Char, b: Char): Boolean {
    val pairs = setOf(
        setOf('3', '8'), setOf('1', '7'), setOf('0', '8'), setOf('5', '6'), setOf('2', '7'), setOf('1', '4')
    )
    return a == b || pairs.any { set -> a in set && b in set }
}
fun closestMatches(input: String, existingIds: List<String>, maxSuggestions: Int = 3): List<String> {
    if (existingIds.isEmpty()) return emptyList()
    val distances = existingIds.map { it to levenshtein(input, it) }
    val minDistance = distances.minOf { it.second }
     return distances.filter { it.second == minDistance }.map { it.first }
}
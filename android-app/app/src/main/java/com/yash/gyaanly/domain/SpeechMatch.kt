package com.yash.gyaanly.domain

import java.util.Locale

/**
 * Scores how closely a recognised utterance matches the target sentence, word by word.
 * Works for any script (Latin with accents, Hangul, Cyrillic…) as long as words are space-separated.
 */
object SpeechMatch {

    data class Result(val score: Int, val missed: List<String>)

    fun compare(target: String, spoken: String): Result {
        val want = words(target)
        val got = words(spoken)
        if (want.isEmpty()) return Result(100, emptyList())
        val lcs = lcsLength(want, got)
        // Penalise both missing and extra words.
        val score = (200.0 * lcs / (want.size + got.size)).toInt().coerceIn(0, 100)
        val gotSet = got.toSet()
        return Result(score, want.filter { it !in gotSet }.distinct())
    }

    private fun words(s: String): List<String> =
        s.lowercase(Locale.ROOT).replace(NON_WORD, " ").split(" ").filter { it.isNotBlank() }

    // Keep letters and digits from every script, plus apostrophes; everything else separates words.
    private val NON_WORD = Regex("[^\\p{L}\\p{M}\\p{N}']+")

    private fun lcsLength(a: List<String>, b: List<String>): Int {
        val dp = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in 1..a.size) for (j in 1..b.size) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1] + 1 else maxOf(dp[i - 1][j], dp[i][j - 1])
        }
        return dp[a.size][b.size]
    }
}

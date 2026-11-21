package com.david.barbellbase.data.update

data class AvailableUpdate(
    val version: String,
    val tag: String,
    val apkUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val releaseNotes: String,
)

data class StoredUpdate(
    val checkedAt: Long = 0L,
    val update: AvailableUpdate? = null,
    val dismissedTag: String? = null,
    val notifiedTag: String? = null,
)

/** Version parsing and comparison for release tags such as "v1.1" and "1.1.2". */
object UpdateVersion {

    fun parse(value: String): List<Int>? {
        val cleaned = value.trim().removePrefix("v").removePrefix("V")
        val core = cleaned.substringBefore('-').substringBefore('+').trim()
        if (core.isEmpty()) return null
        val parts = core.split('.')
        if (parts.size > 4) return null
        return parts.map { it.toIntOrNull() ?: return null }
    }

    fun compare(a: List<Int>, b: List<Int>): Int {
        val size = maxOf(a.size, b.size)
        for (index in 0 until size) {
            val left = a.getOrElse(index) { 0 }
            val right = b.getOrElse(index) { 0 }
            if (left != right) return left.compareTo(right)
        }
        return 0
    }

    fun isNewer(candidate: String, current: String): Boolean {
        val candidateParts = parse(candidate) ?: return false
        val currentParts = parse(current) ?: return false
        return compare(candidateParts, currentParts) > 0
    }
}

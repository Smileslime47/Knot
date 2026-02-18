package moe.saikyo47.rope.text

/**
 * Char sequence's metadata
 */
data class TextMetrics(
    val chars: Int,       // String.length
    val lines: Int,       // '\n' count
    val bytes: Int        // UTF-8 chars
) {
    operator fun plus(other: TextMetrics): TextMetrics = TextMetrics(
        chars + other.chars,
        lines + other.lines,
        bytes + other.bytes
    )

    companion object {
        val ZERO = TextMetrics(0, 0, 0)
    }
}
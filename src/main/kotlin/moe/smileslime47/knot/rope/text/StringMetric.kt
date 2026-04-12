package moe.smileslime47.knot.rope.text

import moe.smileslime47.knot.tree.TreeMetric

/**
 * A metadata calculator which measures String's bytes, linebreaks and chars
 */
object StringMetric : TreeMetric<String, TextMetrics> {
    override val zero: TextMetrics = TextMetrics.ZERO

    override fun measure(value: String): TextMetrics {
        var lines = 0
        var bytes = 0
        for (i in value.indices) {
            val c = value[i]
            if (c == '\n') lines++
            bytes += when {
                c.code <= 0x7F -> 1
                c.code <= 0x7FF -> 2
                c.isHighSurrogate() -> 0
                else -> 3
            }
        }
        return TextMetrics(value.length, lines, bytes)
    }

    override fun combine(a: TextMetrics, b: TextMetrics): TextMetrics = a + b
}
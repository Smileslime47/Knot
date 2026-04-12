package moe.smileslime47.knot.utils

inline fun measureNs(block: () -> Unit): Long {
    val start = System.nanoTime()
    block()
    return System.nanoTime() - start
}

fun nsToMs(ns: Long): String = "%.3f".format(ns / 1_000_000.0)
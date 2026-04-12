package moe.smileslime47.knot.rope.index

import moe.smileslime47.knot.tree.TreeMetric

/**
 * A metadata calculator which only measure a subtree's size
 */
class CountMetric<E> : TreeMetric<E, Int> {
    override val zero: Int = 0
    override fun measure(value: E): Int = 1
    override fun combine(a: Int, b: Int): Int = a + b
}
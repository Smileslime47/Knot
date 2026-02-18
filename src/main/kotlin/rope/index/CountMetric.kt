package moe.saikyo47.rope.index

import moe.saikyo47.tree.TreeMetric

/**
 * A metadata calculator which only measure a subtree's size
 */
class CountMetric<E> : TreeMetric<E, Int> {
    override val zero: Int = 0
    override fun measure(value: E): Int = 1
    override fun combine(a: Int, b: Int): Int = a + b
}
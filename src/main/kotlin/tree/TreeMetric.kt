package moe.saikyo47.tree

interface TreeMetric<E, M> {
    val zero: M

    fun measure(value: E): M

    fun combine(a: M, b: M): M
}
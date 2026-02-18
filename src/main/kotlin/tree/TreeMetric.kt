package moe.saikyo47.tree

/**
 * The calculator to combine the metadata of tree node
 */
interface TreeMetric<E, M> {
    val zero: M

    fun measure(value: E): M

    fun combine(a: M, b: M): M
}
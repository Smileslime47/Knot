package moe.saikyo47.tree

/**
 * 可替换的顺序统计平衡树抽象。
 *
 * T: 存储值类型
 * N: 树节点句柄类型（用于 rank/successor 等操作）
 */
interface OrderStatisticTree<T : Any, N : Any> {
    val size: Int

    fun select(index: Int): N

    fun rank(node: N): Int

    fun insertAt(index: Int, value: T): N

    fun delete(node: N)

    fun insertRange(index: Int, values: Collection<T>, onNodeCreated: (T, N) -> Unit)

    fun deleteRange(index: Int, count: Int, onNodeDeleted: (N) -> Unit)

    fun successorOrNull(node: N): N?
}

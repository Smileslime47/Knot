package moe.saikyo47.tree

/**
 * An abstraction of Order Statistic Tree
 *
 * @param T The type of value stored in the tree
 * @param N The type of the tree node
 */
interface OrderStatisticTree<T : Any, N : TreeNode<T>> {
    val size: Int

    // ========== Query API (Rank / Select) ==========

    /** Finds the node at the specified index. */
    fun select(index: Int): N

    /** Returns the 0-based index of the given node. */
    fun rank(node: N): Int

    // ========== Mutation API ==========

    /** Inserts a value at the specified index. */
    fun insertAt(index: Int, value: T): N

    /** Deletes the node at the specified index. */
    fun delete(node: N)

    // ========== Batch Mutation API ==========
    // Reserved for batch operation optimizations specific to the underlying data structure.

    /** Batch insertion of a collection. */
    fun insertRange(index: Int, values: Collection<T>, onNodeCreated: (T, N) -> Unit){
        var currentIndex = index
        for (value in values) {
            val newNode = insertAt(currentIndex, value)
            onNodeCreated(value, newNode)
            currentIndex++
        }
     }

    /** Batch deletion of a range. */
    fun deleteRange(index: Int, count: Int, onNodeDeleted: (N) -> Unit){
        repeat(count) {
            val node = select(index)
            onNodeDeleted(node)
            delete(node)
        }
    }

    // ========== Visitor ==========

    /** Get the successor node of specified node **/
    fun successorOrNull(node: N): N?{
        val nextIndex = rank(node) + 1
        return if (nextIndex < size) select(nextIndex) else null
    }
}

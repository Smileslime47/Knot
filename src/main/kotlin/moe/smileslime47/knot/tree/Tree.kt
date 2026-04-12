package moe.smileslime47.knot.tree

/**
 * An abstraction of Tree
 *
 * @param E The type of value stored in the tree, which is only non-null in leaf node
 * @param M The type of subtree aggregate metadata
 */
interface Tree<E : Any, M : Any> {
    val root: TreeNode<E, M>?

    val size: Int

    fun <R : Comparable<R>> selectBy(target: R, selector: (M) -> R): TreeNode<E, M>

    fun <R : Comparable<R>> rankBy(node: TreeNode<E, M>, selector: (M) -> R): R

    fun <R : Comparable<R>> insertAt(target: R, value: E, selector: (M) -> R): TreeNode<E, M>

    fun delete(node: TreeNode<E, M>)

    fun <R : Comparable<R>> insertRange(
        target: R,
        values: Collection<E>,
        selector: (M) -> R,
        onNodeCreated: (E, TreeNode<E, M>) -> Unit
    )

    fun <R : Comparable<R>> deleteRange(
        startTarget: R,
        endTarget: R,
        selector: (M) -> R,
        onNodeDeleted: (TreeNode<E, M>) -> Unit
    )

    fun updateValue(node: TreeNode<E, M>, newValue: E)

    fun successorOrNull(node: TreeNode<E, M>): TreeNode<E, M>?
}

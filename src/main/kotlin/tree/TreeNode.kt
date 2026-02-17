package moe.saikyo47.tree

/**
 *
 * @param E element value,which is only non-null in leaf node
 * @param M metadata,to aggregate the metric of subtree
 */
abstract class TreeNode<E : Any, M : Any>(
    var value: E? = null,
    var metadata: M,
) {
    var left: TreeNode<E, M>? = null
    var right: TreeNode<E, M>? = null
    var parent: TreeNode<E, M>? = null
    var size: Int = 1
}

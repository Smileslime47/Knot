package moe.smileslime47.knot.tree

/**
 * A tree's node
 *
 * @param E The type of value stored in the tree, which is only non-null in leaf node
 * @param M The type of subtree aggregate metadata
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

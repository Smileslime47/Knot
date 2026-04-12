package moe.smileslime47.knot.tree.utils

import moe.smileslime47.knot.tree.TreeNode

inline fun <reified T : TreeNode<*, *>> requireNodeType(node: TreeNode<*, *>): T {
    if (node !is T) {
        val expected = T::class.simpleName
        val actual = node::class.simpleName
        throw IllegalArgumentException(
            "Node type mismatch! This Tree implementation expects nodes of type [$expected], but received [$actual]. "
        )
    }
    return node
}
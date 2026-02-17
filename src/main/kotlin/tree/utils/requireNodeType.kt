package moe.saikyo47.tree.utils

import moe.saikyo47.tree.TreeNode

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
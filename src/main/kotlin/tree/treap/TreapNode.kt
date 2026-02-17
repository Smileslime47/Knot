package moe.saikyo47.tree.treap

import moe.saikyo47.tree.TreeNode
import kotlin.random.Random

class TreapNode<E : Any, M : Any>(
    value: E?,
    metadata: M
) : TreeNode<E, M>(value, metadata) {
    val priority: Int = Random.nextInt()
}
package moe.smileslime47.knot.tree.treap

import moe.smileslime47.knot.tree.TreeNode
import kotlin.random.Random

class TreapNode<E : Any, M : Any>(
    value: E?,
    metadata: M
) : TreeNode<E, M>(value, metadata) {
    val priority: Int = Random.nextInt()
}
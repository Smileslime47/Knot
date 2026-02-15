package moe.saikyo47.tree.treap

import moe.saikyo47.tree.TreeNode
import kotlin.random.Random

class TreapNode<T : Any>(value: T?): TreeNode<T>(value) {
    val priority: Int = Random.nextInt()

    var left: TreapNode<T>? = null
    var right: TreapNode<T>? = null
    var parent: TreapNode<T>? = null // 支持 rank 反查

    var size: Int = 1

    override fun toString(): String = "Node($value, p=$priority, s=$size)"
}
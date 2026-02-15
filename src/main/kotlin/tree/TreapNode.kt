package moe.saikyo47.tree

import kotlin.random.Random

class TreapNode<T : Any>(var value: T?) {
    // 核心魔法：随机优先级。
    // 在概率上，几百万个随机数构成的堆，其树高几乎必然是 O(log N) 的。
    val priority: Int = Random.nextInt()

    var left: TreapNode<T>? = null
    var right: TreapNode<T>? = null
    var parent: TreapNode<T>? = null // 支持 rank 反查

    var size: Int = 1

    override fun toString(): String = "Node($value, p=$priority, s=$size)"
}
package moe.saikyo47.rope.index

import moe.saikyo47.tree.TreapNode

/**
 * 行引用：用于将对象与文件的行建立映射关系，比如特定代码的索引
 * value为null用于表示该行无有效索引数据
 *
 * 解绑状态：rope == null 或 node == null
 * - 解绑时 lineNumber 返回 -1
 * - 绑定时 lineNumber 通过 rope.rank(node) 动态计算（O(logN)）
 */

sealed class IndexRef<out E> {
    abstract val index: Int
    open val value: E? get() = (this as? ElementRef)?.value
}


class ElementRef<E>(override val value: E) : IndexRef<E>() {
    internal var rope: IndexedRope<E>? = null // 解绑状态为 null
    internal var node: TreapNode<IndexRef<E>>? = null

    override val index: Int
        get() = if (rope != null && node != null) rope!!.rankOf(node!!) else -1
}


object EmptyRef : IndexRef<Nothing>() {
    override val index: Int = -1 // 空行数据考虑行号无意义
}
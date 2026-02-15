package moe.saikyo47.rope.index

import moe.saikyo47.tree.OrderStatisticTreap
import moe.saikyo47.tree.TreapNode

/**
 * LineRope：行颗粒度的 Rope 平衡树结构
 *
 * - 底层：OrderStatisticRbTree<LineReference<E>> (Pure Split/Join Version)
 * - 支持高效的批量插入 (addAll) 和批量删除 (removeRange)
 * - 维护 ContentReference 的双向绑定关系 (rank 反查能力)
 */
class IndexedRope<E> : AbstractMutableList<IndexRef<E>>() {

    private var tree = OrderStatisticTreap<IndexRef<E>>()
    override val size: Int get() = tree.size

    // ---------------- List 越界检查 ----------------

    private fun checkElementIndex(index: Int) {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("index=$index, size=$size")
        }
    }

    private fun checkPositionIndex(index: Int) {
        if (index < 0 || index > size) {
            throw IndexOutOfBoundsException("index=$index, size=$size")
        }
    }

    // ---------------- 绑定 / 解绑（只针对 ContentReference） ----------------

    private fun ensureInsertable(ref: IndexRef<E>) {
        if (ref is ElementRef) {
            if (ref.rope != null || ref.node != null) {
                throw IllegalStateException(
                    "This ContentReference is already bound to a LineRope/node. " +
                            "Please clone it or remove it from the old rope first."
                )
            }
        }
    }

    // bindIfNeeded 签名变了
    internal fun bindIfNeeded(ref: IndexRef<E>, node: TreapNode<IndexRef<E>>) {
        if (ref is ElementRef) {
            ref.rope = this
            ref.node = node
        }
    }

    internal fun unbindIfNeeded(ref: IndexRef<E>) {
        if (ref is ElementRef) {
            ref.rope = null
            ref.node = null
        }
    }

    // ---------------- AbstractMutableList 实现 ----------------

    override fun get(index: Int): IndexRef<E> {
        checkElementIndex(index)
        return nodeAt(index).value!!
    }

    override fun set(index: Int, element: IndexRef<E>): IndexRef<E> {
        checkElementIndex(index)
        ensureInsertable(element)

        val node = nodeAt(index)
        val old = node.value!!

        // 旧元素解绑
        unbindIfNeeded(old)

        // 替换并绑定新元素
        node.value = element
        bindIfNeeded(element, node)

        return old
    }

    override fun add(index: Int, element: IndexRef<E>) {
        checkPositionIndex(index)
        ensureInsertable(element)

        // 单点插入委托给 tree.insertAt (底层也是 split/join)
        val node = tree.insertAt(index, element)
        bindIfNeeded(element, node)
    }

    override fun removeAt(index: Int): IndexRef<E> {
        checkElementIndex(index)

        // 为了解绑，必须先拿到 node
        val node = nodeAt(index)
        val removed = node.value!!

        unbindIfNeeded(removed)
        tree.delete(node)

        return removed
    }

    // ---------------- 🚀 批量操作优化 (Critical for Performance) ----------------

    override fun addAll(index: Int, elements: Collection<IndexRef<E>>): Boolean {
        checkPositionIndex(index)
        if (elements.isEmpty()) return false

        // 预检查绑定状态
        elements.forEach { ensureInsertable(it) }

        // 委托给树的批量插入，回调处理绑定
        tree.insertRange(index, elements) { ref, node ->
            bindIfNeeded(ref, node)
        }
        return true
    }

    /**
     * 暴露给外部的高效批量删除接口。
     * 虽然 AbstractList 有 protected removeRange，但我们需要 public 入口。
     */
    public override fun removeRange(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex > toIndex) throw IllegalArgumentException("fromIndex($fromIndex) > toIndex($toIndex)")
        checkPositionIndex(fromIndex)
        checkPositionIndex(toIndex)

        val count = toIndex - fromIndex

        // 委托给树的批量删除，回调处理解绑
        tree.deleteRange(fromIndex, count) { node ->
            node.value?.let { unbindIfNeeded(it) }
        }
    }

    override fun clear() {
        if (size > 0) {
            // 使用批量删除清空，比迭代快
            removeRange(0, size)
        }
        // 彻底重置 (防御性)
        tree = OrderStatisticTreap()
    }

    override fun iterator(): MutableIterator<IndexRef<E>> = IndexedRopeIterator(this)

    // ---------------- internal：给 ContentReference.lineNumber 用 ----------------

    internal fun rankOf(node: TreapNode<IndexRef<E>>): Int =
        tree.rank(node)

    internal fun nodeAt(index: Int): TreapNode<IndexRef<E>> =
        tree.select(index)

    internal fun successorOrNull(node: TreapNode<IndexRef<E>>): TreapNode<IndexRef<E>>? =
        tree.successorOrNull(node)

    internal fun deleteNode(node: TreapNode<IndexRef<E>>) =
        tree.delete(node)
}
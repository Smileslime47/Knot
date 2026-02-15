package moe.saikyo47.rope.index

import moe.saikyo47.tree.TreapNode

/**
 * LineRope 的可变迭代器：
 * - next() 返回 LineReference<E>（元素本身）
 * - remove() 删除刚刚返回的元素，并解绑它（rope/node 置空）
 */
class IndexedRopeIterator<E>(
    private val rope: IndexedRope<E>
) : MutableIterator<IndexRef<E>> {

    private var nextNode: TreapNode<IndexRef<E>>? =
        if (rope.isEmpty()) null else rope.nodeAt(0)

    private var lastNode: TreapNode<IndexRef<E>>? = null
    private var canRemove: Boolean = false

    override fun hasNext(): Boolean = nextNode != null

    override fun next(): IndexRef<E> {
        val n = nextNode ?: throw NoSuchElementException()
        lastNode = n
        canRemove = true

        nextNode = rope.successorOrNull(n)
        return n.value!!
    }

    override fun remove() {
        if (!canRemove) throw IllegalStateException("Please call next() before remove()")
        val n = lastNode ?: throw IllegalStateException("Internal state error: lastNode=null")

        val e = n.value!!
        rope.unbindIfNeeded(e)
        rope.deleteNode(n)

        canRemove = false
        lastNode = null
    }
}
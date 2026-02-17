package moe.saikyo47.rope.index

import moe.saikyo47.tree.Tree
import moe.saikyo47.tree.TreeMetric
import moe.saikyo47.tree.TreeNode
import moe.saikyo47.tree.treap.Treap
import java.util.IdentityHashMap


class IndexedRope<E : Any>(
    private val treeFactory: () -> Tree<E, Int> = {
        Treap(CountMetric())
    }
) : AbstractMutableList<E>() {
    private class CountMetric<E> : TreeMetric<E, Int> {
        override val zero: Int = 0
        override fun measure(value: E): Int = 1
        override fun combine(a: Int, b: Int): Int = a + b
    }

    private var tree: Tree<E, Int> = treeFactory()

    private val refMap: IdentityHashMap<E, TreeNode<E, Int>> = IdentityHashMap()

    private val selector: (Int) -> Int = { it }

    override val size: Int get() = tree.size

    private fun checkElementIndex(index: Int) {
        if (index !in 0 until size) {
            throw IndexOutOfBoundsException("index=$index, size=$size")
        }
    }

    private fun checkPositionIndex(index: Int) {
        if (index !in 0..size) {
            throw IndexOutOfBoundsException("index=$index, size=$size")
        }
    }

    private fun ensureNonExist(element: E) {
        if (refMap.containsKey(element)) {
            throw IllegalStateException("Element already bound to node. Remove first.")
        }
    }

    private fun ensureNonExistAndNonDuplicate(elements: Collection<E>) {
        val seen = IdentityHashMap<E, Boolean>(elements.size)
        for (element in elements) {
            ensureNonExist(element)
            if (seen.put(element, true) != null) {
                throw IllegalStateException("Duplicate element in batch insert.")
            }
        }
    }

    // ========== List Operations ==========

    override fun get(index: Int): E {
        checkElementIndex(index)
        return tree.selectBy(index, selector).value!!
    }

    override fun set(index: Int, element: E): E {
        checkElementIndex(index)
        val node = tree.selectBy(index, selector)
        val old = node.value!!
        if (old === element) return old

        ensureNonExist(element)
        refMap.remove(old)

        tree.updateValue(node, element)
        refMap[element] = node
        return old
    }

    override fun add(index: Int, element: E) {
        checkPositionIndex(index)
        ensureNonExist(element)

        val node = tree.insertAt(index, element, selector)
        refMap[element] = node
    }

    override fun removeAt(index: Int): E {
        checkElementIndex(index)
        val node = tree.selectBy(index, selector)
        val removed = node.value!!

        refMap.remove(removed)
        tree.delete(node)
        return removed
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        checkPositionIndex(index)
        if (elements.isEmpty()) return false
        ensureNonExistAndNonDuplicate(elements)

        tree.insertRange(index, elements, selector) { element, node ->
            refMap[element] = node
        }
        return true
    }

    public override fun removeRange(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex > toIndex) throw IllegalArgumentException("fromIndex($fromIndex) > toIndex($toIndex)")
        checkPositionIndex(fromIndex)
        checkPositionIndex(toIndex)

        val count = toIndex - fromIndex
        tree.deleteRange(fromIndex, count, selector) { node ->
            node.value?.let { refMap.remove(it) }
        }
    }

    override fun clear() {
        if (isNotEmpty()) {
            removeRange(0, size)
        }
        tree = treeFactory()
        refMap.clear()
    }

    override fun indexOf(element: E): Int {
        val node = refMap[element] ?: return -1
        return tree.rankBy(node, selector)
    }

    override fun iterator(): MutableIterator<E> = super.iterator()
}
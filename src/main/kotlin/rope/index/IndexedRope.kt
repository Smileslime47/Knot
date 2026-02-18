package moe.saikyo47.rope.index

import moe.saikyo47.tree.Tree
import moe.saikyo47.tree.TreeNode
import moe.saikyo47.tree.treap.Treap
import java.util.IdentityHashMap

/**
 * A List can perform various operations with a time complexity of O(logN), including add, set, get, and indexOf.
 * It is mainly used when an element needs to look up its index in the list.
 * This list does NOT support duplicate element references for now.
 */
class IndexedRope<E : Any>(
    private val treeFactory: () -> Tree<E, Int> = {
        Treap(CountMetric())
    }
) : AbstractMutableList<E>() {
    private var tree: Tree<E, Int> = treeFactory()

    private val refMap: IdentityHashMap<E, TreeNode<E, Int>> = IdentityHashMap()

    private val sizeSelector: (Int) -> Int = { it }

    override val size: Int get() = tree.size

    // ========== List Operations ==========

    override fun get(index: Int): E {
        checkReadIndex(index)

        return tree.selectBy(index, sizeSelector).value!!
    }

    override fun set(index: Int, element: E): E {
        checkReadIndex(index)

        // Check old element
        val node = tree.selectBy(index, sizeSelector)
        val old = node.value!!
        if (old === element) return old

        // Start to set new element if element could be replaced
        ensureNonExist(element)
        tree.updateValue(node, element)

        // Update reference map
        refMap.remove(old)
        refMap[element] = node
        return old
    }

    override fun add(index: Int, element: E) {
        checkWriteIndex(index)

        // Start to insert new lement if element could be inserted
        ensureNonExist(element)
        val node = tree.insertAt(index, element, sizeSelector)

        // Update reference map
        refMap[element] = node
    }

    override fun removeAt(index: Int): E {
        checkReadIndex(index)

        val node = tree.selectBy(index, sizeSelector)
        val removed = node.value!!

        refMap.remove(removed)
        tree.delete(node)
        return removed
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        checkWriteIndex(index)

        if (elements.isEmpty()) return false
        ensureNonExistAndNonDuplicate(elements)

        tree.insertRange(index, elements, sizeSelector) { element, node ->
            refMap[element] = node
        }
        return true
    }

    public override fun removeRange(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        checkWriteRange(fromIndex, toIndex)

        tree.deleteRange(fromIndex, toIndex, sizeSelector) { node ->
            node.value?.let { refMap.remove(it) }
        }
    }

    override fun clear() {
        // Replace the tree with a new tree
        tree = treeFactory()
        refMap.clear()
    }

    override fun indexOf(element: E): Int {
        val node = refMap[element] ?: return -1
        return tree.rankBy(node, sizeSelector)
    }

    override fun iterator(): MutableIterator<E> = super.iterator()

    // ========== Checkers ==========

    private fun checkReadIndex(index: Int) {
        if (index !in 0 until size) {
            throw IndexOutOfBoundsException("index=$index, size=$size")
        }
    }

    private fun checkWriteIndex(index: Int) {
        if (index !in 0..size) {
            throw IndexOutOfBoundsException("index=$index, size=$size")
        }
    }

    private fun checkWriteRange(start: Int, end: Int) {
        if (start < 0 || end > size || start > end) {
            throw IndexOutOfBoundsException("range=[$start, $end], size=$size")
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

}
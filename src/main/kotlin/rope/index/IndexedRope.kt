package moe.saikyo47.rope.index

import moe.saikyo47.tree.OrderStatisticTree
import moe.saikyo47.tree.TreeNode
import moe.saikyo47.tree.treap.OrderStatisticTreap
import java.util.IdentityHashMap

/**
 * A Rope supports operations with O(logN) complexity, including add, set, get, indexOf, and more
 *
 * @param E element
 */
class IndexedRope<E : Any>(
    private val treeFactory: () -> OrderStatisticTree<E, TreeNode<E>> =
        {
            // Pending covariance issues
            @Suppress("UNCHECKED_CAST") (OrderStatisticTreap<E>() as OrderStatisticTree<E, TreeNode<E>>)
        }
) : AbstractMutableList<E>() {
    // The underlying data structure that handles the actual storage and operations
    private var tree: OrderStatisticTree<E, TreeNode<E>> = treeFactory()
    // Maps each element to its corresponding node in the tre
    private val refMap: IdentityHashMap<E, TreeNode<E>> = IdentityHashMap()

    override val size: Int get() = tree.size

    private fun checkElementIndex(index: Int) {
        if (index !in 0..<size) {
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
            throw IllegalStateException(
                "This element is already bound to an IndexedRope node. " +
                    "Please remove it from the old rope first."
            )
        }
    }

    private fun ensureNonExistAndNonDuplicate(elements: Collection<E>) {
        val seen = IdentityHashMap<E, Boolean>(elements.size)
        for (element in elements) {
            ensureNonExist(element)
            if (seen.put(element, true) != null) {
                throw IllegalStateException("Duplicate element reference in the same batch insert.")
            }
        }
    }

    private fun rankOfNode(node: TreeNode<E>): Int {
        val typedTree = tree
        return typedTree.rank(node)
    }

    override fun get(index: Int): E {
        checkElementIndex(index)
        return nodeAt(index).value!!
    }

    override fun set(index: Int, element: E): E {
        checkElementIndex(index)

        val node = nodeAt(index)
        val old = node.value!!
        if (old === element) return old

        ensureNonExist(element)
        refMap.remove(old)
        node.value = element
        refMap[element] = node

        return old
    }

    override fun add(index: Int, element: E) {
        checkPositionIndex(index)
        ensureNonExist(element)

        val node = tree.insertAt(index, element)
        refMap[element] = node
    }

    override fun removeAt(index: Int): E {
        checkElementIndex(index)

        val node = nodeAt(index)
        val removed = node.value!!

        refMap.remove(removed)
        val typedTree = tree
        typedTree.delete(node)

        return removed
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        checkPositionIndex(index)
        if (elements.isEmpty()) return false

        ensureNonExistAndNonDuplicate(elements)
        tree.insertRange(index, elements) { element, node ->
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
        tree.deleteRange(fromIndex, count) { node ->
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

    override fun iterator(): MutableIterator<E> = IndexedRopeIterator(this)

    override fun indexOf(element: E): Int {
        val node = refMap[element] ?: return -1
        return rankOfNode(node)
    }

    internal fun nodeAt(index: Int): TreeNode<E> = tree.select(index)
}

package moe.saikyo47.tree.treap

import moe.saikyo47.tree.Tree
import moe.saikyo47.tree.TreeMetric
import moe.saikyo47.tree.TreeNode
import moe.saikyo47.tree.utils.requireNodeType

class Treap<E : Any, M : Any>(private val metric: TreeMetric<E, M>) : Tree<E, M> {
    private var _root: TreapNode<E, M>? = null
    override val root: TreeNode<E, M>? get() = _root
    override val size: Int get() = _root?.size ?: 0

    private val TreeNode<E, M>?.asTreapNode get() = this as? TreapNode<E, M>

    override fun <R : Comparable<R>> selectBy(target: R, selector: (M) -> R): TreeNode<E, M> {
        var currentNode = _root
        var currentOffset = metric.zero
        while (currentNode != null) {
            val leftSubtreeMetadata = currentNode.left?.metadata ?: metric.zero
            val leftBoundaryRank = metric.combine(currentOffset, leftSubtreeMetadata)

            if (target < selector(leftBoundaryRank)) {
                currentNode = currentNode.left.asTreapNode
            } else {
                val nodeValueMetadata = metric.measure(currentNode.value!!)
                val nodeRightBoundaryRank = metric.combine(leftBoundaryRank, nodeValueMetadata)

                if (target < selector(nodeRightBoundaryRank)) return currentNode

                currentOffset = nodeRightBoundaryRank
                currentNode = currentNode.right.asTreapNode
            }
        }
        throw IndexOutOfBoundsException("Target rank not found in tree")
    }

    override fun <R : Comparable<R>> rankBy(node: TreeNode<E, M>, selector: (M) -> R): R {
        val targetNode = requireNodeType<TreapNode<E, M>>(node)
        var accumulatedMetadata = targetNode.left?.metadata ?: metric.zero
        var currentNode = targetNode

        while (currentNode.parent != null) {
            val parentNode = currentNode.parent.asTreapNode!!
            if (currentNode === parentNode.right) {
                val leftSiblingMetadata = parentNode.left?.metadata ?: metric.zero
                val parentValueMetadata = metric.measure(parentNode.value!!)
                accumulatedMetadata = metric.combine(
                    metric.combine(leftSiblingMetadata, parentValueMetadata),
                    accumulatedMetadata
                )
            }
            currentNode = parentNode
        }
        return selector(accumulatedMetadata)
    }

    override fun <R : Comparable<R>> insertAt(target: R, value: E, selector: (M) -> R): TreeNode<E, M> {
        val newNode = TreapNode(value, metric.measure(value))
        val (leftTree, rightTree) = splitByRank(_root, target, metric.zero, selector)
        _root = merge(merge(leftTree, newNode), rightTree)
        return newNode
    }

    override fun delete(node: TreeNode<E, M>) {
        val targetNode = requireNodeType<TreapNode<E, M>>(node)
        val mergedChildren = merge(
            targetNode.left.asTreapNode?.also { it.parent = null },
            targetNode.right.asTreapNode?.also { it.parent = null }
        )

        replaceInParent(targetNode, mergedChildren)

        var updatePointer = mergedChildren?.parent.asTreapNode ?: targetNode.parent.asTreapNode
        while (updatePointer != null) {
            updateNodeMetrics(updatePointer)
            updatePointer = updatePointer.parent.asTreapNode
        }

        if (_root === targetNode) _root = mergedChildren
        targetNode.parent = null; targetNode.left = null; targetNode.right = null
    }

    override fun updateValue(node: TreeNode<E, M>, newValue: E) {
        val targetNode = requireNodeType<TreapNode<E, M>>(node)
        targetNode.value = newValue
        var updatePointer: TreapNode<E, M>? = targetNode
        while (updatePointer != null) {
            updateNodeMetrics(updatePointer)
            updatePointer = updatePointer.parent.asTreapNode
        }
    }

    override fun <R : Comparable<R>> insertRange(
        target: R,
        values: Collection<E>,
        selector: (M) -> R,
        onNodeCreated: (E, TreeNode<E, M>) -> Unit
    ) {
        if (values.isEmpty()) return
        val (leftTree, rightTree) = splitByRank(_root, target, metric.zero, selector)
        val newSubtree = buildOptimized(values.toList(), 0, values.size - 1, onNodeCreated)
        _root = merge(merge(leftTree, newSubtree), rightTree)
    }

    override fun <R : Comparable<R>> deleteRange(
        target: R,
        count: Int,
        selector: (M) -> R,
        onNodeDeleted: (TreeNode<E, M>) -> Unit
    ) {
        val (leftTree, middleAndRight) = splitByRank(_root, target, metric.zero, selector)
        val (deletedSubtree, rightTree) = splitBySize(middleAndRight, count)
        traverseAndPerform(deletedSubtree, onNodeDeleted)
        _root = merge(leftTree, rightTree)
    }

    override fun successorOrNull(node: TreeNode<E, M>): TreeNode<E, M>? {
        val targetNode = requireNodeType<TreapNode<E, M>>(node)
        if (targetNode.right != null) {
            var leftmost = targetNode.right.asTreapNode!!
            while (leftmost.left != null) leftmost = leftmost.left.asTreapNode!!
            return leftmost
        }
        var current = targetNode
        var ancestor = current.parent.asTreapNode
        while (ancestor != null && current === ancestor.right) {
            current = ancestor
            ancestor = ancestor.parent.asTreapNode
        }
        return ancestor
    }

    private fun merge(left: TreapNode<E, M>?, right: TreapNode<E, M>?): TreapNode<E, M>? = when {
        left == null -> right
        right == null -> left
        left.priority > right.priority -> left.apply {
            this.right = merge(this.right.asTreapNode, right)?.also { it.parent = this }
            updateNodeMetrics(this)
        }

        else -> right.apply {
            this.left = merge(left, this.left.asTreapNode)?.also { it.parent = this }
            updateNodeMetrics(this)
        }
    }

    private fun <R : Comparable<R>> splitByRank(
        node: TreapNode<E, M>?,
        targetRank: R,
        accumulatedOffset: M,
        selector: (M) -> R
    ): Pair<TreapNode<E, M>?, TreapNode<E, M>?> {
        if (node == null) return null to null

        val leftSubtreeMetadata = node.left?.metadata ?: metric.zero
        val currentRank = metric.combine(accumulatedOffset, leftSubtreeMetadata)

        return if (targetRank <= selector(currentRank)) {
            val (leftResult, rightResult) = splitByRank(node.left.asTreapNode, targetRank, accumulatedOffset, selector)
            node.apply { left = rightResult; rightResult?.parent = this; parent = null; updateNodeMetrics(this) }
            leftResult?.apply { parent = null } to node
        } else {
            val selfRank = metric.combine(currentRank, metric.measure(node.value!!))
            val (leftResult, rightResult) = splitByRank(node.right.asTreapNode, targetRank, selfRank, selector)
            node.apply { right = leftResult; leftResult?.parent = this; parent = null; updateNodeMetrics(this) }
            node to rightResult?.apply { parent = null }
        }
    }

    private fun splitBySize(
        node: TreapNode<E, M>?,
        targetSize: Int
    ): Pair<TreapNode<E, M>?, TreapNode<E, M>?> {
        if (node == null) return null to null
        val currentLeftSize = node.left?.size ?: 0

        return if (targetSize <= currentLeftSize) {
            val (leftResult, rightResult) = splitBySize(node.left.asTreapNode, targetSize)
            node.apply { left = rightResult; rightResult?.parent = this; parent = null; updateNodeMetrics(this) }
            leftResult?.apply { parent = null } to node
        } else {
            val (leftResult, rightResult) = splitBySize(node.right.asTreapNode, targetSize - currentLeftSize - 1)
            node.apply { right = leftResult; leftResult?.parent = this; parent = null; updateNodeMetrics(this) }
            node to rightResult?.apply { parent = null }
        }
    }

    private fun updateNodeMetrics(node: TreapNode<E, M>) {
        node.size = 1 + (node.left?.size ?: 0) + (node.right?.size ?: 0)
        val leftMetadata = node.left?.metadata ?: metric.zero
        val selfMetadata = metric.measure(node.value!!)
        val rightMetadata = node.right?.metadata ?: metric.zero
        node.metadata = metric.combine(metric.combine(leftMetadata, selfMetadata), rightMetadata)
    }

    private fun replaceInParent(oldNode: TreapNode<E, M>, newNode: TreapNode<E, M>?) {
        val parent = oldNode.parent.asTreapNode ?: return
        if (parent.left === oldNode) parent.left = newNode else parent.right = newNode
        newNode?.parent = parent
    }

    private fun buildOptimized(
        values: List<E>,
        start: Int,
        end: Int,
        onNodeCreated: (E, TreeNode<E, M>) -> Unit
    ): TreapNode<E, M>? {
        if (start > end) return null
        val mid = (start + end) / 2
        val node = TreapNode(values[mid], metric.measure(values[mid])).also { onNodeCreated(values[mid], it) }
        return merge(
            merge(buildOptimized(values, start, mid - 1, onNodeCreated), node),
            buildOptimized(values, mid + 1, end, onNodeCreated)
        )
    }

    private fun traverseAndPerform(node: TreeNode<E, M>?, action: (TreeNode<E, M>) -> Unit) {
        if (node != null) {
            traverseAndPerform(node.left, action)
            action(node)
            traverseAndPerform(node.right, action)
        }
    }
}
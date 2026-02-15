package moe.saikyo47.tree.treap

import moe.saikyo47.tree.OrderStatisticTree

/**
 * Order Statistic Treap
 */
class OrderStatisticTreap<T : Any> : OrderStatisticTree<T, TreapNode<T>> {

    var root: TreapNode<T>? = null
        private set

    override val size: Int get() = root?.size ?: 0
    fun isEmpty(): Boolean = root == null

    override fun select(index: Int): TreapNode<T> {
        if (index !in 0 until size) throw IndexOutOfBoundsException("index=$index, size=$size")
        var x = root
        var i = index
        while (x != null) {
            val leftSize = x.left?.size ?: 0
            if (i < leftSize) {
                x = x.left
            } else if (i == leftSize) {
                return x
            } else {
                i -= (leftSize + 1)
                x = x.right
            }
        }
        throw IllegalStateException("Select failed")
    }

    override fun rank(node: TreapNode<T>): Int {
        var r = (node.left?.size ?: 0)
        var x = node
        while (x.parent != null) {
            val p = x.parent!!
            if (x === p.right) {
                r += (p.left?.size ?: 0) + 1
            }
            x = p
        }
        return r
    }

    override fun insertAt(index: Int, value: T): TreapNode<T> {
        val node = TreapNode(value)
        val (left, right) = split(root, index)
        // Construction logic: Left + Node + Right
        root = merge(merge(left, node), right)
        return node
    }

    fun deleteAt(index: Int): TreapNode<T> {
        val (left, temp) = split(root, index)
        // Slice out exactly one node from the middle
        val (deleted, right) = split(temp, 1)

        root = merge(left, right)

        val node = deleted ?: throw IllegalStateException("Delete failed")
        node.parent = null // Clear parent reference to prevent memory leaks or stale data
        return node
    }

    override fun delete(node: TreapNode<T>) {
        val index = rank(node)
        deleteAt(index)
    }


    override fun insertRange(index: Int, values: Collection<T>, onNodeCreated: (T, TreapNode<T>) -> Unit) {
        if (values.isEmpty()) return

        // 1. Build a temporary subtree in O(M) time
        val newSubTree = buildFromList(ArrayList(values), 0, values.size - 1, onNodeCreated)

        // 2. Attach to the main tree in O(log N) time
        val (left, right) = split(root, index)
        root = merge(merge(left, newSubTree), right)
    }


    override fun deleteRange(index: Int, count: Int, onNodeDeleted: (TreapNode<T>) -> Unit) {
        if (count <= 0) return
        if (index < 0 || index + count > size) throw IndexOutOfBoundsException()

        // 1. Split into three parts: Left | Deleted | Right
        val (left, temp) = split(root, index)
        val (deletedPart, right) = split(temp, count)

        // 2. Trigger cleanup/notification callbacks
        traverse(deletedPart, onNodeDeleted)

        // 3. Merge remaining parts
        root = merge(left, right)
    }

    // Split, Merge & Build

    /**
     * Merge: Combines trees L and R.
     * Requirement: All indices in L < All indices in R.
     * Uses random priority to maintain heap property and balance.
     */
    private fun merge(l: TreapNode<T>?, r: TreapNode<T>?): TreapNode<T>? {
        if (l == null) return r
        if (r == null) return l

        return if (l.priority > r.priority) {
            val newRight = merge(l.right, r)
            l.right = newRight
            newRight?.parent = l
            updateSize(l)
            l.parent = null // Ensure root parent is always null
            l
        } else {
            val newLeft = merge(l, r.left)
            r.left = newLeft
            newLeft?.parent = r
            updateSize(r)
            r.parent = null
            r
        }
    }

    /**
     * Split: Divides tree 't' at rank 'k'.
     * Returns a Pair(LeftTree, RightTree) where LeftTree.size == k.
     */
    private fun split(t: TreapNode<T>?, k: Int): Pair<TreapNode<T>?, TreapNode<T>?> {
        if (t == null) return null to null

        val leftSize = t.left?.size ?: 0

        return if (k <= leftSize) {
            // Split point is in the left subtree
            val (ll, lr) = split(t.left, k)
            t.left = lr
            lr?.parent = t
            t.parent = null // 't' becomes the root of the right side
            updateSize(t)
            ll?.parent = null // 'll' becomes the root of the left side
            ll to t
        } else {
            // Split point is in the right subtree
            val (rl, rr) = split(t.right, k - leftSize - 1)
            t.right = rl
            rl?.parent = t
            t.parent = null
            updateSize(t)
            rr?.parent = null
            t to rr
        }
    }

    /**
     * Recursive divide-and-conquer construction.
     */
    private fun buildFromList(
        list: List<T>,
        start: Int,
        end: Int,
        onNodeCreated: (T, TreapNode<T>) -> Unit
    ): TreapNode<T>? {
        if (start > end) return null
        if (start == end) {
            val node = TreapNode(list[start])
            onNodeCreated(list[start], node)
            return node
        }

        val mid = (start + end) / 2
        val left = buildFromList(list, start, mid, onNodeCreated)
        val right = buildFromList(list, mid + 1, end, onNodeCreated)

        return merge(left, right)
    }

    // ========== utils ==========

    private fun updateSize(node: TreapNode<T>) {
        node.size = 1 + (node.left?.size ?: 0) + (node.right?.size ?: 0)
    }

    private fun traverse(node: TreapNode<T>?, action: (TreapNode<T>) -> Unit) {
        if (node == null) return
        traverse(node.left, action)
        action(node)
        traverse(node.right, action)
    }

    fun firstOrNull(): TreapNode<T>? {
        var x = root ?: return null
        while (x.left != null) x = x.left!!
        return x
    }

    override fun successorOrNull(node: TreapNode<T>): TreapNode<T>? {
        var x = node
        if (x.right != null) {
            var curr = x.right!!
            while (curr.left != null) curr = curr.left!!
            return curr
        }
        var y = x.parent
        while (y != null && x === y.right) {
            x = y
            y = y.parent
        }
        return y
    }
}
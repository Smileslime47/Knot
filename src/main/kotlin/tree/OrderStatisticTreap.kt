package moe.saikyo47.tree

/**
 * 顺序统计树堆 (Pure Split/Merge Version)
 *
 * 设计哲学：
 * 1. 极简主义：移除所有旋转 (Rotation) 和迭代 (Classic) 逻辑。
 * 2. 逻辑统一：单点操作只是批量操作的特例，一切增删皆为 Split/Merge。
 * 3. 稳定性：完全依赖随机优先级 (Random Priority) 维护平衡，无复杂的不变量校验。
 */
internal class OrderStatisticTreap<T : Any> {

    var root: TreapNode<T>? = null
        private set

    val size: Int get() = root?.size ?: 0
    fun isEmpty(): Boolean = root == null

    // ============================================================
    // 查询 API (Rank / Select) - 保持 O(log N)
    // ============================================================

    fun select(index: Int): TreapNode<T> {
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

    fun rank(node: TreapNode<T>): Int {
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

    // ============================================================
    // 增删 API (全部基于 Split/Merge)
    // ============================================================

    /** 单点插入 */
    fun insertAt(index: Int, value: T): TreapNode<T> {
        val node = TreapNode(value)
        val (left, right) = split(root, index)
        // 逻辑：Left + Node + Right
        root = merge(merge(left, node), right)
        return node
    }

    /** 单点删除 */
    fun deleteAt(index: Int): TreapNode<T> {
        val (left, temp) = split(root, index)
        // 逻辑：从中间切出 1 个
        val (deleted, right) = split(temp, 1)

        root = merge(left, right)

        val node = deleted ?: throw IllegalStateException("Delete failed")
        node.parent = null // 断开引用，防止内存泄漏或脏数据
        return node
    }

    fun delete(node: TreapNode<T>) {
        val index = rank(node)
        deleteAt(index)
    }

    /** 批量插入 */
    fun insertRange(index: Int, values: Collection<T>, onNodeCreated: (T, TreapNode<T>) -> Unit) {
        if (values.isEmpty()) return

        // 1. O(M) 构建临时树
        val newSubTree = buildFromList(ArrayList(values), 0, values.size - 1, onNodeCreated)

        // 2. O(log N) 接入主树
        val (left, right) = split(root, index)
        root = merge(merge(left, newSubTree), right)
    }

    /** 批量删除 */
    fun deleteRange(index: Int, count: Int, onNodeDeleted: (TreapNode<T>) -> Unit) {
        if (count <= 0) return
        if (index < 0 || index + count > size) throw IndexOutOfBoundsException()

        // 1. 切三段：Left | Deleted | Right
        val (left, temp) = split(root, index)
        val (deletedPart, right) = split(temp, count)

        // 2. 触发回调
        traverse(deletedPart, onNodeDeleted)

        // 3. 合并剩余
        root = merge(left, right)
    }

    // ============================================================
    // 核心魔法：Split & Merge & Build
    // ============================================================

    /**
     * Merge: 合并两棵树 L 和 R。
     * 假设 L 所有元素索引 < R 所有元素索引。
     * 利用随机优先级决定谁做根，从而概率性保证平衡。
     */
    private fun merge(l: TreapNode<T>?, r: TreapNode<T>?): TreapNode<T>? {
        if (l == null) return r
        if (r == null) return l

        if (l.priority > r.priority) {
            // L 优先级高，L 做根，L.right = merge(L.right, R)
            val newRight = merge(l.right, r)
            l.right = newRight
            newRight?.parent = l
            updateSize(l)
            l.parent = null // 根节点 parent 必须清空
            return l
        } else {
            // R 优先级高，R 做根，R.left = merge(L, R.left)
            val newLeft = merge(l, r.left)
            r.left = newLeft
            newLeft?.parent = r
            updateSize(r)
            r.parent = null
            return r
        }
    }

    /**
     * Split: 将树 t 在 rank k 处切开。
     * 返回 (LeftTree, RightTree)，其中 LeftTree.size == k。
     */
    private fun split(t: TreapNode<T>?, k: Int): Pair<TreapNode<T>?, TreapNode<T>?> {
        if (t == null) return null to null

        val leftSize = t.left?.size ?: 0

        if (k <= leftSize) {
            // 切点在左子树
            val (ll, lr) = split(t.left, k)
            t.left = lr
            lr?.parent = t
            t.parent = null // t 成为右半边的根
            updateSize(t)
            ll?.parent = null // ll 成为左半边的根
            return ll to t
        } else {
            // 切点在右子树
            val (rl, rr) = split(t.right, k - leftSize - 1)
            t.right = rl
            rl?.parent = t
            t.parent = null
            updateSize(t)
            rr?.parent = null
            return t to rr
        }
    }

    /**
     * 递归分治构建：利用 merge 自动平衡
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

        // 直接 merge，Treap 堆性质会自动生效
        return merge(left, right)
    }

    // ============================================================
    // Helpers
    // ============================================================

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

    fun successorOrNull(node: TreapNode<T>): TreapNode<T>? {
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
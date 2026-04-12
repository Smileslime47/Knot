package moe.smileslime47.knot.tree

import moe.smileslime47.knot.tree.treap.Treap
import moe.smileslime47.knot.tree.treap.TreapNode
import java.util.*
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TreapTest {
    class Entry(val value: Int)

    private lateinit var tree: Treap<Entry, Int>
    private val selector: (Int) -> Int = { it }

    private val countMetric = object : TreeMetric<Entry, Int> {
        override val zero: Int = 0
        override fun measure(value: Entry): Int = 1
        override fun combine(a: Int, b: Int): Int = a + b
    }

    @BeforeTest
    fun setup() {
        tree = Treap(countMetric)
    }

    @Test
    fun testBasicInsertionAndOrder() {
        val e10 = Entry(10)
        val e20 = Entry(20)
        val e30 = Entry(30)

        tree.insertAt(0, e10, selector)
        tree.insertAt(0, e20, selector)
        tree.insertAt(1, e30, selector)

        assertEquals(3, tree.size)
        val result = collectInOrder(tree.root)
        assertEquals(listOf(20, 30, 10), result.map { it.value })
        validateTreapStructure(tree.root)
    }

    @Test
    fun testSelectAndRankConsistency() {
        val count = 50
        val entries = (0 until count).map { Entry(it * 10) }
        entries.forEachIndexed { index, entry ->
            tree.insertAt(index, entry, selector)
        }

        for (i in 0 until count) {
            val node = tree.selectBy(i, selector)
            assertEquals(entries[i].value, node.value!!.value)
            assertEquals(i, tree.rankBy(node, selector))
        }
    }

    @Test
    fun testDeleteRange() {
        (0..6).forEach { tree.insertAt(it, Entry(it), selector) }

        tree.deleteRange(2, 5, selector) { }

        val values = collectInOrder(tree.root).map { it.value }
        assertEquals(listOf(0, 1, 5, 6), values)
        validateTreapStructure(tree.root)
    }

    @Test
    fun testInsertRange() {
        tree.insertAt(0, Entry(0), selector)
        tree.insertAt(1, Entry(3), selector)

        val range = listOf(Entry(1), Entry(2))
        tree.insertRange(1, range, selector) { _, _ -> }

        val values = collectInOrder(tree.root).map { it.value }
        assertEquals(listOf(0, 1, 2, 3), values)
        validateTreapStructure(tree.root)
    }

    @Test
    fun testRandomFuzzOperations() {
        repeat(20) {
            val referenceList = LinkedList<Entry>()
            val nodeMap = IdentityHashMap<Entry, TreeNode<Entry, Int>>()
            val random = Random(System.nanoTime())
            val iterations = 500

            repeat(iterations) {
                val opType = random.nextInt(4)
                val currentSize = referenceList.size

                try {
                    when (opType) {
                        0 -> {
                            val value = Entry(random.nextInt())
                            val index = random.nextInt(currentSize + 1)
                            referenceList.add(index, value)
                            val node = tree.insertAt(index, value, selector)
                            nodeMap[value] = node
                        }
                        1 -> {
                            if (currentSize > 0) {
                                val index = random.nextInt(currentSize)
                                val valueToRemove = referenceList.removeAt(index)
                                val nodeToRemove = nodeMap.remove(valueToRemove)!!
                                tree.delete(nodeToRemove)
                            }
                        }
                        2 -> {
                            val index = random.nextInt(currentSize + 1)
                            val count = random.nextInt(5) + 1
                            val entries = (0 until count).map { Entry(random.nextInt()) }
                            referenceList.addAll(index, entries)
                            tree.insertRange(index, entries, selector) { entry, node ->
                                nodeMap[entry] = node
                            }
                        }
                        3 -> {
                            if (currentSize > 0) {
                                val start = random.nextInt(currentSize)
                                val maxCount = currentSize - start
                                val count = random.nextInt(maxCount) + 1
                                val end = start + count

                                repeat(count) {
                                    val removed = referenceList.removeAt(start)
                                    nodeMap.remove(removed)
                                }
                                tree.deleteRange(start, end, selector) { }
                            }
                        }
                    }
                    assertEquals(referenceList.size, tree.size)
                } catch (e: Exception) {
                    throw e
                }
            }

            val actualValues = collectInOrder(tree.root).map { it.value }
            val expectedValues = referenceList.map { it.value }
            assertEquals(expectedValues, actualValues)
            validateTreapStructure(tree.root)

            tree = Treap(countMetric)
        }
    }

    private fun collectInOrder(root: TreeNode<Entry, Int>?): List<Entry> {
        val result = mutableListOf<Entry>()
        fun dfs(node: TreeNode<Entry, Int>?) {
            if (node == null) return
            dfs(node.left)
            result.add(node.value!!)
            dfs(node.right)
        }
        dfs(root)
        return result
    }

    private fun validateTreapStructure(node: TreeNode<Entry, Int>?) {
        if (node == null) return
        val tNode = node as TreapNode<Entry, Int>
        val left = tNode.left as? TreapNode<Entry, Int>
        val right = tNode.right as? TreapNode<Entry, Int>

        if (left != null) {
            assertTrue(tNode.priority >= left.priority)
            assertSame(tNode, left.parent)
            validateTreapStructure(left)
        }
        if (right != null) {
            assertTrue(tNode.priority >= right.priority)
            assertSame(tNode, right.parent)
            validateTreapStructure(right)
        }

        val expectedSize = 1 + (left?.size ?: 0) + (right?.size ?: 0)
        assertEquals(expectedSize, tNode.size)

        val leftMeta = left?.metadata ?: countMetric.zero
        val rightMeta = right?.metadata ?: countMetric.zero
        val selfMeta = countMetric.measure(tNode.value!!)
        val expectedMeta = countMetric.combine(countMetric.combine(leftMeta, selfMeta), rightMeta)
        assertEquals(expectedMeta, tNode.metadata)
    }
}
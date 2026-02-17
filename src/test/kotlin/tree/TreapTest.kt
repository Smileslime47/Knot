package moe.saikyo47.tree

import moe.saikyo47.tree.treap.Treap
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.random.Random

class TreapTest {
    class Entry(val v: Int)

    private lateinit var tree: Treap<Entry, Int>
    private val selector: (Int) -> Int = { it }

    private val metric = object : TreeMetric<Entry, Int> {
        override val zero: Int = 0
        override fun measure(value: Entry): Int = 1
        override fun combine(a: Int, b: Int): Int = a + b
    }

    @BeforeEach
    fun setup() {
        tree = Treap(metric)
    }

    @Test
    fun `test insert and order`() {
        val list = listOf(10, 20, 30).map { Entry(it) }
        tree.insertAt(0, list[0], selector) // [10]
        tree.insertAt(0, list[1], selector) // [20, 10]
        tree.insertAt(1, list[2], selector) // [20, 30, 10]

        val actual = mutableListOf<Int>()
        inOrder(tree.root) { actual.add(it.value!!.v) }
        assertEquals(listOf(20, 30, 10), actual)
    }

    @Test
    fun `test range operations`() {
        (0..4).forEach { tree.insertAt(it, Entry(it), selector) }
        tree.insertRange(2, listOf(Entry(100), Entry(200)), selector) { _, _ -> }

        val actual = mutableListOf<Int>()
        inOrder(tree.root) { actual.add(it.value!!.v) }
        assertEquals(listOf(0, 1, 100, 200, 2, 3, 4), actual)
    }


    @RepeatedTest(20)
    fun `random fuzz test with identity safety`() {
        val reference = LinkedList<Entry>()
        val nodeMap = IdentityHashMap<Entry, TreeNode<Entry, Int>>()
        val random = Random(System.nanoTime())

        repeat(1000) {
            val op = random.nextInt(4)
            when (op) {
                0 -> { // Insert
                    val e = Entry(random.nextInt())
                    val idx = if (reference.isEmpty()) 0 else random.nextInt(reference.size + 1)
                    reference.add(idx, e)
                    nodeMap[e] = tree.insertAt(idx, e, selector)
                }

                1 -> { // Delete (Single Node)
                    if (reference.isNotEmpty()) {
                        val e = reference.removeAt(random.nextInt(reference.size))
                        val node = nodeMap.remove(e) ?: throw IllegalStateException("Node not in map!")
                        tree.delete(node)
                    }
                }

                2 -> { // Update
                    if (reference.isNotEmpty()) {
                        val idx = random.nextInt(reference.size)
                        val oldE = reference[idx]
                        val newE = Entry(random.nextInt())
                        reference[idx] = newE
                        val node = nodeMap.remove(oldE)!!
                        tree.updateValue(node, newE)
                        nodeMap[newE] = node
                    }
                }

                3 -> { // DeleteRange
                    if (reference.isNotEmpty()) {
                        val idx = random.nextInt(reference.size)
                        val count = random.nextInt(reference.size - idx) + 1
                        repeat(count) {
                            val e = reference.removeAt(idx)
                            nodeMap.remove(e)
                        }
                        tree.deleteRange(idx, count, selector) {}
                    }
                }
            }
            assertEquals(reference.size, tree.size, "Size mismatch after op $op")
        }

        val actual = mutableListOf<Entry>()
        inOrder(tree.root) { actual.add(it.value!!) }
        assertEquals(reference.size, actual.size)
        for (i in reference.indices) {
            assertSame(reference[i], actual[i], "Identity mismatch at index $i")
        }
    }

    private fun inOrder(node: TreeNode<Entry, Int>?, action: (TreeNode<Entry, Int>) -> Unit) {
        if (node == null) return
        inOrder(node.left, action)
        action(node)
        inOrder(node.right, action)
    }
}
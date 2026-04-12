package moe.smileslime47.knot.rope.text

import moe.smileslime47.knot.tree.Tree
import moe.smileslime47.knot.tree.TreeNode
import moe.smileslime47.knot.tree.treap.Treap
import kotlin.math.min

/**
 * A data structure for storing large-scale strings.
 * It maintains O(logN) performance when inserting large texts of 1MB in size.
 */
class CharRope(
    private val treeFactory: () -> Tree<String, TextMetrics> = {
        Treap(StringMetric)
    }
) : CharSequence {
    private companion object {
        const val MAX_CHUNK_SIZE = 512
    }

    private var tree: Tree<String, TextMetrics> = treeFactory()

    private val charSelector: (TextMetrics) -> Int = { it.chars }
    private val lineSelector: (TextMetrics) -> Int = { it.lines }

    override val length: Int get() = tree.root?.metadata?.chars ?: 0
    val lineCount: Int get() = (tree.root?.metadata?.lines ?: 0) + 1
    val byteSize: Int get() = tree.root?.metadata?.bytes ?: 0

    override fun get(index: Int): Char {
        checkReadIndex(index)

        // get the chunk of specified index
        val (node, offset) = findNodeWithCharOffset(index)
        return node.value!![index - offset]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        if (startIndex == endIndex) return ""
        checkWriteRange(startIndex, endIndex)

        val sb = StringBuilder(endIndex - startIndex)
        var current = startIndex
        while (current < endIndex) {
            val (node, offset) = findNodeWithCharOffset(current)
            val chunk = node.value!!
            val localStart = current - offset
            val localEnd = min(chunk.length, localStart + (endIndex - current))
            sb.append(chunk, localStart, localEnd)
            current += (localEnd - localStart)
        }
        return sb.toString()
    }

    fun insert(index: Int, text: String): CharRope {
        if (text.isEmpty()) return this

        if (index == length) return append(text)
        checkWriteIndex(index)

        val (node, offset) = findNodeWithCharOffset(index)
        val localIndex = index - offset
        val originalText = node.value!!

        tree.delete(node)

        val prefix = originalText.substring(0, localIndex)
        val suffix = originalText.substring(localIndex)
        val newNodes = mutableListOf<String>()

        if (prefix.isNotEmpty()) newNodes.add(prefix)

        // Auto-Chunking: 将插入的长文本切碎
        chunkString(text, newNodes)

        if (suffix.isNotEmpty()) newNodes.add(suffix)

        tree.insertRange(offset, newNodes, charSelector) { _, _ -> }
        return this
    }

    fun delete(startIndex: Int, endIndex: Int): CharRope {
        if (startIndex == endIndex) return this
        checkWriteRange(startIndex, endIndex)

        splitNodeAt(startIndex)
        splitNodeAt(endIndex)

        tree.deleteRange(startIndex, endIndex, charSelector) { }
        return this
    }

    fun append(text: String): CharRope {
        if (text.isEmpty()) return this

        if (text.length > MAX_CHUNK_SIZE) {
            val chunks = ArrayList<String>(text.length / MAX_CHUNK_SIZE + 1)
            chunkString(text, chunks)
            tree.insertRange(length, chunks, charSelector) { _, _ -> }
        } else {
            tree.insertAt(length, text, charSelector)
        }
        return this
    }

    fun getLineStart(lineIndex: Int): Int {
        if (lineIndex == 0) return 0
        checkReadLine(lineIndex)

        val targetNewlineRank = lineIndex - 1
        val node = tree.selectBy(targetNewlineRank, lineSelector)

        val nodeStartRank = tree.rankBy(node, charSelector)
        val nodeValues = node.value!!
        val linesBeforeNode = tree.rankBy(node, lineSelector)

        var localNewlinesFound = 0
        val targetLocalNewlines = targetNewlineRank - linesBeforeNode

        for (i in nodeValues.indices) {
            if (nodeValues[i] == '\n') {
                if (localNewlinesFound == targetLocalNewlines) {
                    return nodeStartRank + i + 1
                }
                localNewlinesFound++
            }
        }
        return nodeStartRank
    }

    private fun chunkString(text: String, destination: MutableList<String>) {
        var offset = 0
        while (offset < text.length) {
            val end = min(offset + MAX_CHUNK_SIZE, text.length)
            destination.add(text.substring(offset, end))
            offset = end
        }
    }

    private fun findNodeWithCharOffset(absIndex: Int): Pair<TreeNode<String, TextMetrics>, Int> {
        val node = tree.selectBy(absIndex, charSelector)
        val nodeStartIndex = tree.rankBy(node, charSelector)
        return node to nodeStartIndex
    }

    private fun splitNodeAt(absIndex: Int) {
        if (absIndex == 0 || absIndex == length) return

        val (node, offset) = findNodeWithCharOffset(absIndex)
        if (offset == absIndex) return

        val localIndex = absIndex - offset
        val text = node.value!!

        val pre = text.substring(0, localIndex)
        val post = text.substring(localIndex)

        tree.delete(node)
        tree.insertRange(offset, listOf(pre, post), charSelector) { _, _ -> }
    }

    override fun toString(): String {
        val sb = StringBuilder(length)
        fun dfs(n: TreeNode<String, TextMetrics>?) {
            if (n == null) return
            dfs(n.left)
            sb.append(n.value)
            dfs(n.right)
        }
        dfs(tree.root)
        return sb.toString()
    }

    // ========== Checkers ==========

    private fun checkReadLine(lineIndex: Int) {
        if (lineIndex !in 0 until lineCount) {
            throw IndexOutOfBoundsException("lineIndex=$lineIndex, lines=$lineCount")
        }
    }

    private fun checkReadIndex(index: Int) {
        if (index !in 0 until length) {
            throw IndexOutOfBoundsException("index=$index, length=$length")
        }
    }

    private fun checkWriteIndex(index: Int) {
        if (index !in 0..length) {
            throw IndexOutOfBoundsException("index=$index, length=$length")
        }
    }

    private fun checkWriteRange(start: Int, end: Int) {
        if (start < 0 || end > length || start > end) {
            throw IndexOutOfBoundsException("range=[$start, $end], length=$length")
        }
    }
}
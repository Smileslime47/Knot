package moe.smileslime47.knot.rope.text

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CharRopeTest {

    @Test
    fun testBasicInsertionAndAppend() {
        val rope = CharRope()
        assertEquals(0, rope.length)

        rope.append("Hello")
        assertEquals("Hello", rope.toString())

        rope.insert(5, " World")
        assertEquals("Hello World", rope.toString())

        rope.insert(0, "Start: ")
        assertEquals("Start: Hello World", rope.toString())

        // 中间插入
        rope.insert(7, "Beautiful ")
        // "Start: Beautiful Hello World"
        assertEquals("Start: Beautiful Hello World", rope.toString())
    }

    @Test
    fun testDeletion() {
        val rope = CharRope()
        rope.append("Hello World") // len=11

        // 删除 " World"
        rope.delete(5, 11)
        assertEquals("Hello", rope.toString())

        // 删除 "He"
        rope.delete(0, 2)
        assertEquals("llo", rope.toString())

        // 全删
        rope.delete(0, rope.length)
        assertEquals(0, rope.length)
        assertEquals("", rope.toString())
    }

    @Test
    fun testRandomAccessAndSubSequence() {
        val str = "0123456789"
        val rope = CharRope().apply { append(str) }

        // 测试 get(index)
        for (i in str.indices) {
            assertEquals(str[i], rope[i], "Character mismatch at index $i")
        }

        // 测试 subSequence
        assertEquals("234", rope.subSequence(2, 5).toString())
        assertEquals("01", rope.subSequence(0, 2).toString())
        assertEquals("89", rope.subSequence(8, 10).toString())
    }

    @Test
    fun testLineMetricsAndSearch() {
        // 构建一个多行文本:
        // Line 0: "A\n" (0-1)
        // Line 1: "BC\n" (2-4)
        // Line 2: "DEF"  (5-7)
        val rope = CharRope()
        rope.append("A\nBC\nDEF")

        assertEquals(8, rope.length)
        assertEquals(3, rope.lineCount, "Line count mismatch")

        // 测试 getLineStart
        assertEquals(0, rope.getLineStart(0)) // Line 0 starts at 0
        assertEquals(2, rope.getLineStart(1)) // Line 1 starts at 2 ('B')
        assertEquals(5, rope.getLineStart(2)) // Line 2 starts at 5 ('D')

        // 边界测试：插入导致行号变化
        rope.insert(0, "\n")
        // Now: "\nA\nBC\nDEF" -> 4 lines
        assertEquals(4, rope.lineCount)
        assertEquals(0, rope.getLineStart(0))
        assertEquals(1, rope.getLineStart(1)) // Old line 0 is now line 1
    }

    @Test
    fun testBoundaryConditions() {
        val rope = CharRope()

        // 空串操作
        rope.delete(0, 0)
        assertEquals("", rope.toString())

        // 越界检查
        assertFailsWith<IndexOutOfBoundsException> {
            rope.get(0)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            rope.insert(1, "Fail")
        }
        assertFailsWith<IndexOutOfBoundsException> {
            rope.getLineStart(100)
        }
    }

    @Test
    fun testMixedOperationsConsistency() {
        // 与 StringBuilder 进行随机对标
        val sb = StringBuilder()
        val rope = CharRope()
        val random = Random(12345)

        repeat(1000) {
            val op = random.nextInt(3)
            when (op) {
                0 -> { // Append
                    val txt = random.nextInt(100).toString()
                    sb.append(txt)
                    rope.append(txt)
                }
                1 -> { // Insert
                    val txt = "X"
                    val idx = if (sb.isEmpty()) 0 else random.nextInt(sb.length + 1)
                    sb.insert(idx, txt)
                    rope.insert(idx, txt)
                }
                2 -> { // Delete
                    if (sb.isNotEmpty()) {
                        val start = random.nextInt(sb.length)
                        val end = start + random.nextInt(sb.length - start) + 1
                        val safeEnd = end.coerceAtMost(sb.length)
                        sb.delete(start, safeEnd)
                        rope.delete(start, safeEnd)
                    }
                }
            }
            assertEquals(sb.length, rope.length)
        }
        assertEquals(sb.toString(), rope.toString())
    }
}
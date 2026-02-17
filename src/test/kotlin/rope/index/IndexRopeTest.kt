package moe.saikyo47.rope.index

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IndexRopeTest {
    private class Entry(val id: Int) {
        override fun toString(): String = "Entry($id)"
    }

    @Test
    fun addGetAndIndexOfUseIdentitySemantics() {
        val rope = IndexedRope<Entry>()
        val a = Entry(1)
        val b = Entry(1)
        val c = Entry(2)

        rope.add(0, a)
        rope.add(1, c)

        assertEquals(2, rope.size)
        assertEquals(0, rope.indexOf(a))
        assertEquals(-1, rope.indexOf(b))
        assertEquals(1, rope.indexOf(c))
        assertEquals(a, rope[0])
        assertEquals(c, rope[1])
    }

    @Test
    fun addAtMiddleKeepsOrder() {
        val rope = IndexedRope<Entry>()
        val a = Entry(1)
        val b = Entry(2)
        val c = Entry(3)

        rope.add(0, a)
        rope.add(1, c)
        rope.add(1, b)

        assertContentEquals(listOf(a, b, c), rope.toList())
    }

    @Test
    fun addRejectsDuplicateReference() {
        val rope = IndexedRope<Entry>()
        val a = Entry(1)

        rope.add(0, a)
        assertFailsWith<IllegalStateException> { rope.add(1, a) }
    }

    @Test
    fun addAllRejectsExistingAndBatchDuplicateReferences() {
        val rope = IndexedRope<Entry>()
        val existing = Entry(1)
        val x = Entry(2)
        val y = Entry(3)

        rope.add(0, existing)

        assertFailsWith<IllegalStateException> {
            rope.addAll(1, listOf(x, existing))
        }
        assertFailsWith<IllegalStateException> {
            rope.addAll(1, listOf(y, y))
        }
    }

    @Test
    fun addAllInsertsAtPositionAndReturnsExpectedBoolean() {
        val rope = IndexedRope<Entry>()
        val a = Entry(1)
        val b = Entry(2)
        val c = Entry(3)
        val d = Entry(4)

        rope.add(0, a)
        rope.add(1, d)
        assertFalse(rope.addAll(1, emptyList()))
        assertTrue(rope.addAll(1, listOf(b, c)))

        assertContentEquals(listOf(a, b, c, d), rope.toList())
        assertEquals(1, rope.indexOf(b))
        assertEquals(2, rope.indexOf(c))
    }

    @Test
    fun setReplacesReferenceAndMaintainsReverseLookup() {
        val rope = IndexedRope<Entry>()
        val a = Entry(1)
        val b = Entry(2)
        val c = Entry(3)

        rope.addAll(0, listOf(a, b))
        val old = rope.set(0, c)

        assertEquals(a, old)
        assertEquals(-1, rope.indexOf(a))
        assertEquals(0, rope.indexOf(c))
        assertEquals(1, rope.indexOf(b))
        assertContentEquals(listOf(c, b), rope.toList())
    }

    @Test
    fun setSameReferenceIsNoOp() {
        val rope = IndexedRope<Entry>()
        val a = Entry(1)

        rope.add(0, a)
        val old = rope.set(0, a)

        assertEquals(a, old)
        assertEquals(0, rope.indexOf(a))
        assertContentEquals(listOf(a), rope.toList())
    }

    @Test
    fun setRejectsReferenceAlreadyInRope() {
        val rope = IndexedRope<Entry>()
        val a = Entry(1)
        val b = Entry(2)

        rope.addAll(0, listOf(a, b))
        assertFailsWith<IllegalStateException> { rope.set(0, b) }
    }

    @Test
    fun removeAtUpdatesLookupAndOrder() {
        val rope = IndexedRope<Entry>()
        val a = Entry(1)
        val b = Entry(2)
        val c = Entry(3)

        rope.addAll(0, listOf(a, b, c))
        val removed = rope.removeAt(1)

        assertEquals(b, removed)
        assertEquals(-1, rope.indexOf(b))
        assertEquals(0, rope.indexOf(a))
        assertEquals(1, rope.indexOf(c))
        assertContentEquals(listOf(a, c), rope.toList())
    }

    @Test
    fun removeRangeHandlesNoOpAndBoundaryValidation() {
        val rope = IndexedRope<Entry>()
        val values = listOf(Entry(1), Entry(2), Entry(3), Entry(4), Entry(5))
        rope.addAll(0, values)

        rope.subList(2, 2).clear()
        assertContentEquals(values, rope.toList())

        rope.subList(1, 4).clear()
        assertContentEquals(listOf(values[0], values[4]), rope.toList())
        assertEquals(-1, rope.indexOf(values[1]))
        assertEquals(-1, rope.indexOf(values[2]))
        assertEquals(-1, rope.indexOf(values[3]))

        assertFailsWith<IllegalArgumentException> {
            rope.removeRange(1, 0)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            rope.removeRange(-1, 0)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            rope.removeRange(0, 3)
        }
    }

    @Test
    fun clearEmptiesRopeAndAllowsReinsertOfSameReference() {
        val rope = IndexedRope<Entry>()
        val a = Entry(1)
        val b = Entry(2)

        rope.addAll(0, listOf(a, b))
        rope.clear()

        assertTrue(rope.isEmpty())
        assertEquals(-1, rope.indexOf(a))
        assertEquals(-1, rope.indexOf(b))

        rope.add(0, a)
        assertEquals(0, rope.indexOf(a))
    }

    @Test
    fun iteratorRemoveContractAndCursorBehavior() {
        val rope = IndexedRope<Entry>()
        val a = Entry(1)
        val b = Entry(2)
        val c = Entry(3)
        rope.addAll(0, listOf(a, b, c))

        val it = rope.iterator()
        assertFailsWith<IllegalStateException> { it.remove() }

        assertEquals(a, it.next())
        it.remove()
        assertContentEquals(listOf(b, c), rope.toList())
        assertFailsWith<IllegalStateException> { it.remove() }

        assertEquals(b, it.next())
        assertEquals(c, it.next())
        it.remove()
        assertContentEquals(listOf(b), rope.toList())
        assertFalse(it.hasNext())
    }

    @Test
    fun indexValidationForGetAddAndRemove() {
        val rope = IndexedRope<Entry>()
        val a = Entry(1)

        assertFailsWith<IndexOutOfBoundsException> { rope.get(0) }
        assertFailsWith<IndexOutOfBoundsException> { rope.removeAt(0) }
        assertFailsWith<IndexOutOfBoundsException> { rope.add(1, a) }

        rope.add(0, a)
        assertFailsWith<IndexOutOfBoundsException> { rope.get(-1) }
        assertFailsWith<IndexOutOfBoundsException> { rope.get(1) }
        assertFailsWith<IndexOutOfBoundsException> { rope.removeAt(1) }
        assertFailsWith<IndexOutOfBoundsException> { rope.add(-1, Entry(2)) }
    }
}

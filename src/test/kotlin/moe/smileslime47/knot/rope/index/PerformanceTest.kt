package moe.smileslime47.knot.rope.index

import moe.smileslime47.knot.utils.measureNs
import moe.smileslime47.knot.utils.nsToMs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class PerformanceTest {
    private val baseSize = 10_000_000
    private val insertOps = 10_000
    private val reverseLookupOps = 10_000

    // To ensure the element is identity-based compared
    private class Entry(val id: Int)

    @Test
    fun benchmarkInsertPerformance() {
        println("[Insert Benchmark] Preparing data (Size: $baseSize)")
        val base = (0 until baseSize).map { Entry(it) }

        val list = ArrayList<Entry>(baseSize + insertOps).apply { addAll(base) }
        val rope = IndexedRope<Entry>().apply { addAll(0, base) }

        // prepare operations for test
        val inserts = (baseSize until baseSize + insertOps).map { Entry(it) }
        val positionRandom = Random(20260215)
        val insertPositions = IntArray(insertOps) { i -> positionRandom.nextInt(baseSize + i + 1) }

        println("[Insert Benchmark] Starting tests (Edits: $insertOps)")

        // List test
        val listInsertNs = measureNs {
            for (i in 0 until insertOps) {
                list.add(insertPositions[i], inserts[i])
            }
        }

        // IndexedRope test
        val ropeInsertNs = measureNs {
            for (i in 0 until insertOps) {
                rope.add(insertPositions[i], inserts[i])
            }
        }

        assertEquals(list.size, rope.size, "Size mismatch after insertion workload")

        println("List insert (O(N)):            ${nsToMs(listInsertNs)} ms")
        println("IndexedRope insert (O(log N)): ${nsToMs(ropeInsertNs)} ms")
        println("Insertion Speedup:             ${"%.2f".format(listInsertNs.toDouble() / ropeInsertNs)}x")
    }

    @Test
    fun benchmarkLookupPerformance() {
        println("[Lookup Benchmark] Preparing data (Size: $baseSize)")
        val base = (0 until baseSize).map { Entry(it) }

        val list = ArrayList<Entry>(baseSize + insertOps).apply { addAll(base) }
        val rope = IndexedRope<Entry>().apply { addAll(0, base) }

        val queryRandom = Random(20260216)
        val queryElements = Array(reverseLookupOps) {
            val idx = queryRandom.nextInt(list.size)
            list[idx]
        }

        println("[Lookup Benchmark] Starting tests (Lookups: $reverseLookupOps)")

        // List test
        var listChecksum = 0L
        val listLookupNs = measureNs {
            for (element in queryElements) {
                listChecksum += list.indexOf(element).toLong()
            }
        }

        // IndexedRope test
        var ropeChecksum = 0L
        val ropeLookupNs = measureNs {
            for (element in queryElements) {
                ropeChecksum += rope.indexOf(element).toLong()
            }
        }

        assertEquals(listChecksum, ropeChecksum, "Reverse lookup checksum mismatch")

        println("List scan for line (O(N)):             ${nsToMs(listLookupNs)} ms")
        println("IndexedRope getLineStart (O(log N)):   ${nsToMs(ropeLookupNs)} ms")
        println("Line Lookup Speedup:                   ${"%.2f".format(listChecksum.toDouble() / ropeLookupNs)}x")
    }
}

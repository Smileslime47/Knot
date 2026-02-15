package moe.saikyo47.rope.index

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class PerformanceTest {
    // Keep identity-based equality for indexOf(element) comparison.
    private class Entry(val id: Int)

    @Test
    fun insertAndReverseLookupTest() {
        // Large-scale workload (>= 1e5).
        val baseSize = 1_000_000
        val insertOps = 2_000
        val reverseLookupOps = 1_000_000

        // Deterministic input for reproducible benchmark output.
        val base = (0 until baseSize).map { Entry(it) }
        val inserts = (baseSize until baseSize + insertOps).map { Entry(it) }
        val positionRandom = Random(20260215)
        val insertPositions = IntArray(insertOps) { i -> positionRandom.nextInt(baseSize + i + 1) }

        val list = ArrayList<Entry>(baseSize + insertOps).apply { addAll(base) }
        val rope = IndexedRope<Entry>().apply { addAll(0, base) }

        val listInsertNs = measureNs {
            for (i in 0 until insertOps) {
                list.add(insertPositions[i], inserts[i])
            }
        }
        val ropeInsertNs = measureNs {
            for (i in 0 until insertOps) {
                rope.add(insertPositions[i], inserts[i])
            }
        }

        assertEquals(list.size, rope.size, "Size mismatch after insertion workload")

        // Reverse lookup is based on elements, not random indexes.
        // Both containers use the same queried element instances for fairness.
        val queryRandom = Random(20260216)
        val queryElements = Array(reverseLookupOps) {
            val idx = queryRandom.nextInt(list.size)
            list[idx]
        }

        var listChecksum = 0L
        val listReverseLookupNs = measureNs {
            for (element in queryElements) {
                listChecksum += list.indexOf(element).toLong()
            }
        }

        var ropeChecksum = 0L
        val ropeReverseLookupNs = measureNs {
            for (element in queryElements) {
                ropeChecksum += rope.indexOf(element).toLong()
            }
        }

        // Verify both implementations produce the same logical lookup result.
        assertEquals(listChecksum, ropeChecksum, "Reverse lookup checksum mismatch")

        println("=== Performance Comparison (base=$baseSize, inserts=$insertOps, reverseLookups=$reverseLookupOps) ===")
        println("ArrayList insert: ${nsToMs(listInsertNs)} ms")
        println("IndexedRope insert: ${nsToMs(ropeInsertNs)} ms")
        println("ArrayList reverse lookup(indexOf): ${nsToMs(listReverseLookupNs)} ms")
        println("IndexedRope reverse lookup(indexOf): ${nsToMs(ropeReverseLookupNs)} ms")
    }

    private inline fun measureNs(block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return System.nanoTime() - start
    }

    private fun nsToMs(ns: Long): String = "%.3f".format(ns / 1_000_000.0)
}

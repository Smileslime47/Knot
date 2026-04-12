package moe.smileslime47.knot.rope.text

import moe.smileslime47.knot.utils.measureNs
import moe.smileslime47.knot.utils.nsToMs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class CharRopePerformanceTest {
    private val docSize = 100_000_000
    private val editOperations = 10_000
    private val lineLookupOps = 10_000

    @Test
    fun benchmarkInsertionPerformance() {
        println("[Insert Benchmark] Preparing data (Size: $docSize)")
        val initialText = generateInitialText(docSize)

        val stringBuilder = StringBuilder(initialText)
        val charRope = CharRope().apply { append(initialText) }

        // prepare operations for test
        val rng = Random(20260218)
        val insertPositions = IntArray(editOperations) {
            rng.nextInt(docSize + it)
        }
        val insertTexts = Array(editOperations) { "X" }

        println("[Insert Benchmark] Starting tests (Edits: $editOperations)")

        // StringBuilder test
        val sbInsertNs = measureNs {
            for (i in 0 until editOperations) {
                stringBuilder.insert(insertPositions[i], insertTexts[i])
            }
        }

        // CharRope test
        val ropeInsertNs = measureNs {
            for (i in 0 until editOperations) {
                charRope.insert(insertPositions[i], insertTexts[i])
            }
        }

        assertEquals(stringBuilder.length, charRope.length, "Length mismatch after edits")

        println("StringBuilder insert (O(N)):   ${nsToMs(sbInsertNs)} ms")
        println("CharRope insert (O(log N)):    ${nsToMs(ropeInsertNs)} ms")
        println("Insertion Speedup:             ${"%.2f".format(sbInsertNs.toDouble() / ropeInsertNs)}x")
    }

    @Test
    fun benchmarkLineLookupPerformance() {
        println("[Lookup Benchmark] Preparing data (Size: $docSize)")

        val testString = generateInitialText(docSize)
        val rope = CharRope().apply { append(testString) }

        val queryRandom = Random(20260218)
        val totalLines = rope.lineCount
        val randomLines = IntArray(lineLookupOps) { queryRandom.nextInt(totalLines) }

        println("[Lookup Benchmark] Starting tests (Lookups: $lineLookupOps)")

        // StringBuilder test
        var stringCheckSum = 0L
        val stringLookupNs = measureNs {
            for (lineIndex in randomLines) {
                stringCheckSum += manualScanLineStart(testString, lineIndex).toLong()
            }
        }

        // CharRope test
        var ropeCheckSum = 0L
        val ropeLookupNs = measureNs {
            for (lineIndex in randomLines) {
                ropeCheckSum += rope.getLineStart(lineIndex).toLong()
            }
        }

        assertEquals(stringCheckSum, ropeCheckSum, "Lookup checksum mismatch!")

        println("String scan for line (O(N)):       ${nsToMs(stringLookupNs)} ms")
        println("CharRope getLineStart (O(log N)):  ${nsToMs(ropeLookupNs)} ms")
        println("Line Lookup Speedup:               ${"%.2f".format(stringLookupNs.toDouble() / ropeLookupNs)}x")
    }

    private fun generateInitialText(size: Int): String {
        val lineContent = "a".repeat(49) + "\n"
        val builder = StringBuilder(size + 100)
        while (builder.length < size) {
            builder.append(lineContent)
        }
        return builder.toString()
    }

    private fun manualScanLineStart(text: String, lineIndex: Int): Int {
        if (lineIndex == 0) return 0
        var linesFound = 0
        for (i in text.indices) {
            if (text[i] == '\n') {
                linesFound++
                if (linesFound == lineIndex) return i + 1
            }
        }
        return -1
    }
}
import java.math.BigInteger

fun main() {
    val testInput = readInput("Day06_test")
    val input = readInput("Day06")

    check(part1(testInput) == BigInteger.valueOf( 4277556))
    part1(input).println()

    check(part2(testInput) == BigInteger.valueOf(3263827))
    part2(input).println()
}

private fun part1(input: List<String>): BigInteger {
val lines = input
    // Pad all lines to the same width so column indexing is safe
    val width = lines.maxOf { it.length }
    val padded = lines.map { it.padEnd(width, ' ') }

    // Determine which columns are entirely spaces (blank separators)
    val columnBlank = BooleanArray(width) { col ->
        padded.all { it[col] == ' ' }
    }

    // Find contiguous non-blank column spans -> each is one problem block
    val spans = mutableListOf<IntRange>()
    var c = 0
    while (c < width) {
        if (!columnBlank[c]) {
            val start = c
            while (c < width && !columnBlank[c]) c++
            spans.add(start until c)
        } else {
            c++
        }
    }

    val lastRowIndex = padded.lastIndex
    var grandTotal = BigInteger.ZERO

    for (span in spans) {
        val numbers = mutableListOf<BigInteger>()

        // Collect numbers from all rows except the last (which contains the operator)
        for (r in 0 until lastRowIndex) {
            val slice = padded[r].substring(span.first, span.last + 1).trim()
            if (slice.isNotEmpty()) {
                // parse integer
                numbers.add(BigInteger(slice))
            }
        }

        // Find operator: first non-space char in the last row slice
        val opSlice = padded[lastRowIndex].substring(span.first, span.last + 1)
        val opChar = opSlice.trim().firstOrNull()
            ?: throw IllegalArgumentException("No operator found in block at columns ${span.first}-${span.last}")

        val blockResult = when (opChar) {
            '+' -> numbers.fold(BigInteger.ZERO) { acc, n -> acc + n }
            '*' -> numbers.fold(BigInteger.ONE) { acc, n -> acc * n }
            else -> throw IllegalArgumentException("Unknown operator '$opChar' in block at columns ${span.first}-${span.last}")
        }

        grandTotal += blockResult
    }

    return (grandTotal)
}


private fun part2(input: List<String>): BigInteger {
    val lines = input
    // Pad all lines to the same width so column indexing is safe
    val width = lines.maxOf { it.length }
    val padded = lines.map { it.padEnd(width, ' ') }

    // Determine which columns are entirely spaces (blank separators)
    val columnBlank = BooleanArray(width) { col ->
        padded.all { it[col] == ' ' }
    }

    // Find contiguous non-blank column spans -> each is one problem block
    val spans = mutableListOf<IntRange>()
    var c = 0
    while (c < width) {
        if (!columnBlank[c]) {
            val start = c
            while (c < width && !columnBlank[c]) c++
            spans.add(start until c)
        } else {
            c++
        }
    }

    val lastRowIndex = padded.lastIndex
    var grandTotal = BigInteger.ZERO

    for (span in spans) {
        val numbers = mutableListOf<BigInteger>()

        // For Part Two: each column inside the span is a number.
        // Read digits top-to-bottom from rows 0..lastRowIndex-1, trim spaces.
        // Then read columns right-to-left to get the cephalopod order.
        for (col in span.first..span.last) {
            val sb = StringBuilder()
            for (r in 0 until lastRowIndex) {
                sb.append(padded[r][col])
            }
            val numStr = sb.toString().trim()
            if (numStr.isNotEmpty()) {
                numbers.add(BigInteger(numStr))
            } else {
                // If a column has no digits (all spaces above), skip it.
            }
        }

        // Reverse to read right-to-left (cephalopod order)
        numbers.reverse()

        // Find operator: first non-space char in the last row slice
        val opSlice = padded[lastRowIndex].substring(span.first, span.last + 1)
        val opChar = opSlice.trim().firstOrNull()
            ?: throw IllegalArgumentException("No operator found in block at columns ${span.first}-${span.last}")

        val blockResult = when (opChar) {
            '+' -> numbers.fold(BigInteger.ZERO) { acc, n -> acc + n }
            '*' -> numbers.fold(BigInteger.ONE) { acc, n -> acc * n }
            else -> throw IllegalArgumentException("Unknown operator '$opChar' in block at columns ${span.first}-${span.last}")
        }

        grandTotal += blockResult
    }

    return (grandTotal)
}

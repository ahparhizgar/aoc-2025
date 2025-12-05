fun main() {
    val testInput = readInput("Day05_test")
    val input = readInput("Day05")

    check(part1(testInput) == 3)
    part1(input).println()

    check(part2(testInput) == 14L)
    part2(input).println()
}

private fun part1(input: List<String>): Int {
    val ranges = mutableSetOf<LongRange>()
    var freshCount = 0
    var rangesEnded = false
    input.forEach {
        if (it.isBlank()) {
            rangesEnded = true
            return@forEach
        }
        if (!rangesEnded) {
            ranges.add(it.toLongRange())
        } else {
            val number = it.toLong()
            if (ranges.any { range -> number in range }) {
                freshCount++
            }
        }
    }
    return freshCount
}

private fun String.toLongRange(): LongRange {
    val (start, end) = this.split("-").map { it.toLong() }
    return start..end
}

private fun part2(input: List<String>): Long {
    val ranges = mutableListOf<LongRange>()
    input.forEach { range ->
        if (range.isBlank()) {
            ranges.sortBy { it.first }
            return removeConflicts(ranges).sumOf { it.size }
        }
        val (start, end) = range.split("-").map { it.toLong() }
        ranges.add(start..end)
    }
    error("No blank line found")
}

private val LongRange.size: Long
    get() = last - first + 1

private fun removeConflicts(sortedRanges: List<LongRange>): List<LongRange> {
    val noConflictRanges = mutableListOf<LongRange>()
    sortedRanges.forEach { sortedRange ->
        if (noConflictRanges.isEmpty()) {
            noConflictRanges.add(sortedRange)
        } else {
            val lastRange = noConflictRanges.last()
            if (sortedRange.first <= lastRange.last) {
                noConflictRanges.removeLast()
                noConflictRanges.add(lastRange.first..maxOf(lastRange.last, sortedRange.last))
            } else {
                noConflictRanges.add(sortedRange)
            }
        }
    }
    return noConflictRanges
}


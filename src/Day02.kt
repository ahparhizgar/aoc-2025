fun main() {
    val testInput = readInput("Day02_test")
    val input = readInput("Day02")

    check(part1(testInput) == 1227775554L)
    part1(input).println()

    check(part2(testInput) == 4174379265L)
    part2(input).println()
}

private fun part1(input: List<String>): Long {
    val ranges = createRanges(input)
    var sum = 0L
    for (number in ranges.flatten()) {
        val numberStr = number.toString()
        if (numberStr.length % 2 == 0) {
            val halfLength = numberStr.length / 2
            val left = numberStr.take(halfLength)
            val right = numberStr.drop(halfLength)
            if (left == right) {
                sum += number
            }
        }
    }
    return sum
}

private fun part2(input: List<String>): Long {
    val ranges = createRanges(input)
    var sum = 0L
    for (number in ranges.flatten()) {
        val numberStr = number.toString()
        for (partLength in 1..<numberStr.length) {
            if (numberStr.length % partLength == 0) {
                if (numberStr.chunked(partLength).allSame()) {
                    sum += number
                    break
                }
            }
        }
    }

    return sum
}

private fun List<*>.allSame(): Boolean =
    this.distinct().size <= 1

private fun createRanges(input: List<String>): List<LongRange> =
    input.single()
        .split(",")
        .map { range ->
            range.split("-")
                .map { it.toLong() }
                .let { it.first()..it.last() }
        }

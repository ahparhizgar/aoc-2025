fun main() {
    val testInput = readInput("Day03_test")
    val input = readInput("Day03")

    check(part1(testInput) == 357L)
    part1(input).println()

    check(part2(testInput) == 3121910778619L)
    part2(input).println()
}

private fun part1(input: List<String>): Long {
    return input.sumOf { line ->
        if (line.length < 2) throw IllegalArgumentException("Each bank must contain at least two batteries")

        var maxPrefixDigit = -1
        var bestPair = -1
        for (j in 1 until line.length) {
            val prevDigit = line[j - 1] - '0'
            if (prevDigit > maxPrefixDigit) maxPrefixDigit = prevDigit
            val curDigit = line[j] - '0'
            val value = 10 * maxPrefixDigit + curDigit
            if (value > bestPair) bestPair = value
        }
        bestPair.toLong()
    }
}

private fun part2(input: List<String>): Long {
    val k = 12
    return input.sumOf { line ->
        val n = line.length
        if (n < k) throw IllegalArgumentException("Each bank must contain at least $k batteries")

        val stack = ArrayList<Char>(k)
        for (i in line.indices) {
            val c = line[i]
            while (stack.isNotEmpty() && stack.size + (n - i) > k && stack.last() < c) {
                stack.removeAt(stack.size - 1)
            }
            if (stack.size < k) {
                stack.add(c)
            }
        }
        val chosen = if (stack.size > k) stack.subList(0, k) else stack

        var value = 0L
        for (ch in chosen) {
            value = value * 10 + (ch - '0')
        }
        value
    }
}

fun main() {
    val testInput = readInput("Day03_test")
    val input = readInput("Day03")

    check(part1(testInput) == 357L)
    part1(input).println()

    check(part2(testInput) == 3121910778619)
    part2(input).println()
}

private fun part1(input: List<String>): Long {
    return input.sumOf { maxPower(bank = it).toLong() }
}

fun maxPower(bank: String): String {
    with(bank) {
        val max = max()
        val indexOfMax = indexOf(max)
        return if (indexOfMax != lastIndex) {
            val secondMax = drop(indexOfMax + 1).max()
            "$max$secondMax"
        } else {
            val newMax = dropLast(1).max()
            "$newMax$max"
        }
    }
}

private fun part2(input: List<String>): Long {
    return input.sumOf { maxPowerN(it, 12).toLong() }
}

fun maxPowerN(bank: String, digits: Int): String {
    with(bank) {
        if (digits == 1) {
            return max().toString()
        }
        val max = dropLast(digits - 1).max().toString()
        return max + maxPowerN(bank.substring(indexOf(max) + 1), digits - 1)
    }
}

fun maxPowerNTail(bank: String, digits: Int): String {
    with(bank) {
        if (digits == 1) {
            return max().toString()
        }
        val max = dropLast(digits - 1).max().toString()
        return max + maxPowerN(bank.substring(indexOf(max) + 1), digits - 1)
    }
}

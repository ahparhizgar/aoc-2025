fun main() {
    val testInput = readInput("Day04_test")
    val input = readInput("Day04")

    part1(testInput).println()
    check(part1(testInput) == 13)
    part1(input).println()

    check(part2(testInput) == 43)
    part2(input).println()
}

private fun part1(diagram: List<String>): Int {
    return diagram.countPapers() - diagram.removePapers().countPapers()
}

private fun List<String>.countPapers(): Int =
    this.sumOf { row -> row.count { it.isPaper() } }

private fun Char?.isPaper() = this == '@'

private val adjacentPositions = listOf(
    Pair(-1, 0), Pair(1, 0),
    Pair(0, -1), Pair(0, 1),
    Pair(-1, -1), Pair(-1, 1),
    Pair(1, -1), Pair(1, 1),
)

private fun List<String>.removePapers(): List<String> {
    val result = this.toMutableList()
    forEachIndexed { i, row ->
        row.forEachIndexed { j, position ->
            if (position.isPaper()) {
                val adjacentPapers = adjacentPositions.count { (x, y) ->
                    getOrNull(i + x, j + y).isPaper()
                }
                if (adjacentPapers < 4) {
                    result.removePaper(i, j)
                }
            }
        }
    }
    return result
}

private fun MutableList<String>.removePaper(i: Int, j: Int) {
    this[i] = this[i].replaceRange(j, j + 1, ".")
}

private fun List<String>.getOrNull(i: Int, j: Int): Char? =
    this.getOrNull(i)?.getOrNull(j)

private fun part2(input: List<String>): Int {
    var diagram = input
    while (true) {
        val count = diagram.countPapers()
        diagram = diagram.removePapers()
        val newCount = diagram.countPapers()
        if (newCount == count) {
            return input.countPapers() - newCount
        }
    }
}


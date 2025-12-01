import java.lang.Math.floorMod

fun main() {

    val testInput = readInput("Day01_test")
    check(part1(testInput) == 3)

    val input = readInput("Day01")
    part1(input).println()
    part2(input).println()
}

fun part1(input: List<String>): Int {
    var result = 0
    input.fold(50) { currentValue, line ->
        val sign = when (line.substring(0, 1)) {
            "R" -> 1
            "L" -> -1
            else -> error("invalid character in $line")
        }
        val value = line.substring(1).toInt()
        floorMod(currentValue + sign * value, 100).also {
            if (it == 0) {
                result++
            }
        }
    }
    return result
}


fun part2(input: List<String>): Int {
    return input.size
}

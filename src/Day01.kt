import java.lang.Math.floorMod

fun main() {

    val testInput = readInput("Day01_test")
    check(part1(testInput) == 3)

    val input = readInput("Day01")
    part1(input).println()
    check(part2(testInput) == 6)
    part2(input).println()
}

fun part1(input: List<String>): Int {
    var result = 0
    input.fold(50) { currentValue, line ->
        floorMod(currentValue + line.sign() * line.value(), 100).also {
            if (it == 0) {
                result++
            }
        }
    }
    return result
}

fun part2(input: List<String>): Int {
    var result = 0
    input.fold(50) { currentValue, line ->
        val inc = line.sign() * line.value()
        floorMod(currentValue + inc, 100).also {
            result += if (line.sign() > 0) {
                (currentValue + inc) / 100
            } else {
                ((100 - currentValue) % 100 - inc) / 100
            }
        }
    }
    return result
}

private fun String.value(): Int = this.substring(1).toInt()

private fun String.sign(): Int = when (this.first()) {
    'R' -> 1
    'L' -> -1
    else -> error("invalid character ${this.first()}")
}

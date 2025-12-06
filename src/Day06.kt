import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.operations.fold
import org.jetbrains.kotlinx.multik.ndarray.operations.map
import org.jetbrains.kotlinx.multik.ndarray.operations.toArray

fun main() {
    val testInput = readInput("Day06_test")
    val input = readInput("Day06")

    check(part1(testInput) == 4277556L)
    part1(input).println()

    check(part2(testInput) == 3263827L)
    part2(input).println()
}

private fun part1(input: List<String>): Long {
    val grid = input.map { line ->
        line.trim().split("\\s+".toRegex())
    }
    val numbersGrid = mk.ndarray(
        grid.dropLast(1)
            .map { row -> row.map { it.toLong() }.toLongArray() }
            .toTypedArray())

    val operations = grid.last()
    val problems = numbersGrid.transpose().toArray()
        .mapIndexed { index, longs -> Pair(operations[index], longs) }

    return problems.sumOf { (operation, numbers) ->
        when (operation) {
            "*" -> numbers.reduce(Long::times)
            "+" -> numbers.reduce(Long::plus)
            else -> error("unknown operation $operation")
        }
    }
}

private fun part2(input: List<String>): Long {
    val maxLength = input.maxOf { it.length }
    val a = mk.ndarray(input.map { c -> c.padEnd(maxLength).map { c -> c.code } }).transpose()
//    println(a.toArray().joinToString("\n") { it.map { it.toChar() }.joinToString("") })
    val b =  a.toArray().fold(emptyList<Pair<Char, LongArray>>()) { acc, ints ->
        if (ints.all { it == ' '.code }) {
            return@fold acc
        }
        val operation = ints.last().toChar()
        if (operation == '+' || operation == '*') {
            acc + Pair(operation, ints.dropLast(1).map { it.toChar() }.joinToString("").trim().split("\\s+".toRegex()).map { it.toLong() }.toLongArray())
        } else {
           val newOne =  Pair(acc.last().first, acc.last().second + ints.map { it.toChar() }.joinToString("").trim().split("\\s+".toRegex()).map { it.toLong() }.toLongArray())
            acc.dropLast(1) + newOne
        }
    }
    return b.sumOf { (operation, numbers) ->
        when (operation) {
            '*' -> numbers.reduce(Long::times)
            '+' -> numbers.reduce(Long::plus)
            else -> error("unknown operation $operation")
        }
    }
}

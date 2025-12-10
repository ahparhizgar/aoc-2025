import kotlin.math.max
import kotlin.math.min

fun main() {
    val testInput = readInput("Day10_test")
    val input = readInput("Day10")
    check(part1(testInput) == 7L)
    part1(input).println()

//    check(part2(testInput) == 33L)
//    part2(input).println()
}

private fun part1(input: List<String>): Long {
    var total = 0L
    val diagRegex = Regex("\\[([.#]+)\\]")
    val buttonRegex = Regex("\\(([^)]*)\\)")

    for (line in input) {
        if (line.isBlank()) continue

        val diagMatch = diagRegex.find(line) ?: error("No indicator diagram found in line: $line")
        val diagram = diagMatch.groupValues[1]
        val n = diagram.length

        // Build target mask
        var target = 0L
        for (i in 0 until n) {
            if (diagram[i] == '#') {
                target = target or (1L shl i)
            }
        }

        // Parse buttons -> bitmasks
        val keepMask = if (n == 64) -1L else (1L shl n) - 1L
        val buttonMasks = mutableListOf<Long>()
        for (bm in buttonRegex.findAll(line)) {
            val inner = bm.groupValues[1].trim()
            if (inner.isEmpty()) continue
            var mask = 0L
            inner.split(',').forEach { tok ->
                val s = tok.trim()
                if (s.isNotEmpty()) {
                    val idx = s.toInt()
                    require(idx >= 0) { "Negative index in button: $s" }
                    // Ignore bits beyond n to keep state space to 2^n
                    if (idx < 64) {
                        mask = mask or (1L shl idx)
                    } else {
                        // Indices >= 64 can't be represented in Long state; they don't affect the first n lights anyway.
                        // We just drop them to keep BFS over n-light subspace.
                    }
                }
            }
            mask = mask and keepMask
            if (mask != 0L) buttonMasks.add(mask)
        }

        // Deduplicate buttons to reduce branching
        val uniqueButtons = buttonMasks.toMutableSet().toList()

        // Trivial case: already at target
        if (target == 0L) {
            // zero presses needed
            continue
        }

        // If there are no buttons but target != 0 => impossible
        require(uniqueButtons.isNotEmpty()) { "No buttons available to reach non-zero target in line: $line" }

        var min = Int.MAX_VALUE
        val all = powerSet(uniqueButtons.toSet())
        all.forEach { subset ->
            var state = 0L
            for (btn in subset) {
                state = state xor btn
            }
            if (state == target) {
                min = min(subset.size, min)
                return@forEach
            }
        }

        total += min
    }

    return total
}

// functional / fold approach
fun <T> powerSet(s: Set<T>): List<Set<T>> =
    s.fold(listOf(emptySet())) { acc, e -> acc + acc.map { it + e } }// bitmask approach (deterministic order)

fun <T> powerSetBitmask(s: Set<T>): List<Set<T>> {
    val list = s.toList()
    val n = list.size
    val result = ArrayList<Set<T>>(1 shl n)
    for (mask in 0 until (1 shl n)) {
        val subset = HashSet<T>()
        for (i in 0 until n) if ((mask and (1 shl i)) != 0) subset.add(list[i])
        result.add(subset)
    }
    return result
}


data class TripleKey(val a: Int, val b: Int, val c: Int)

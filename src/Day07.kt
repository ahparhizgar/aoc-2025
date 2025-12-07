fun main() {
    val testInput = readInput("Day07_test")
    val input = readInput("Day07")
    check(part1(testInput) == 21)
    part1(input).println()

    check(part2(testInput) == 40L)
    part2(input).println()
}

private fun part1(input: List<String>): Int {
    return input.fold(BeamStep(emptySet(), 0)) { step, line ->
        step.modify {
            line.forEachIndexed { index, c ->
                when (c) {
                    'S' -> sourceAt(index)
                    '^' -> separatorAt(index)
                }
            }
        }
    }.splits
}

class BeamStep(val beamIndexes: Set<Int>, val splits: Int) {
    fun modify(block: Scope.() -> Unit): BeamStep {
        val newBeamIndexes = beamIndexes.toMutableSet()
        var splitAddons = 0
        val scope = object : Scope {
            override fun separatorAt(index: Int) {
                if (newBeamIndexes.remove(index)) {
                    newBeamIndexes.add(index - 1)
                    newBeamIndexes.add(index + 1)
                    splitAddons++
                }
            }

            override fun sourceAt(index: Int) {
                newBeamIndexes.add(index)
            }
        }
        block(scope)
        return BeamStep(beamIndexes = newBeamIndexes, splits = splits + splitAddons)
    }

    interface Scope {
        fun separatorAt(index: Int)
        fun sourceAt(index: Int)
    }
}

private fun part2(input: List<String>): Long =
    input.reversed().fold(List(input.first().length) { 1L }) { lastStep, line ->
        line.mapIndexed { index, c ->
            if (lastStep.isEmpty()) {
                1L
            } else {
                when (c) {
                    '^' -> lastStep[index - 1] + lastStep[index + 1]
                    else -> lastStep[index]
                }
            }
        }
    }[input.first().indexOf('S')]


// slow and recursive solution for part 2
private fun part2Slow(input: List<String>): Long {
    return recursiveSolution(input = input.drop(1), beamIndex = input.first().indexOf('S'))
}

private fun recursiveSolution(input: List<String>, beamIndex: Int): Long {
    if (input.isEmpty()) return 1
    return when (input.first()[beamIndex]) {
        '^' -> recursiveSolution(
            input = input.drop(1),
            beamIndex = beamIndex - 1
        ) + recursiveSolution(
            input = input.drop(1),
            beamIndex = beamIndex + 1
        )

        else -> {
            return recursiveSolution(
                input = input.drop(1),
                beamIndex = beamIndex
            )
        }
    }
}

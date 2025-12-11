fun main() {
    val testInput = readInput("Day11_test")
    val input = readInput("Day11")

    check(part1(testInput) == 5L)
    part1(input).println()

    part2(
        """
        svr: aaa bbb
        aaa: fft
        fft: ccc
        bbb: tty
        tty: ccc
        ccc: ddd eee
        ddd: hub
        hub: fff
        eee: dac
        dac: fff
        fff: ggg hhh
        ggg: out
        hhh: out
    """.trimIndent().lines()
    ).println()
//    check(part2(testInput) == 2L)
    part2(input).println()
}


private fun parseGraph(input: List<String>): Map<String, List<String>> {
    val graph = mutableMapOf<String, MutableList<String>>()
    for (line in input) {
        if (line.isBlank()) continue
        val parts = line.split(":", limit = 2)
        val node = parts[0].trim()
        val outs = if (parts.size > 1) {
            parts[1].trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        } else emptyList()
        graph.computeIfAbsent(node) { mutableListOf() }.addAll(outs)
        for (t in outs) graph.computeIfAbsent(t) { mutableListOf() }
    }
    return graph
}

private fun part1(input: List<String>): Long {
    val graph = parseGraph(input)

    fun dfs(curr: String, visited: MutableSet<String>): Long {
        if (curr == "out") return 1L
        visited.add(curr)
        var count = 0L
        for (n in graph[curr].orEmpty()) {
            if (!visited.contains(n)) count += dfs(n, visited)
        }
        visited.remove(curr)
        return count
    }

    if (!graph.containsKey("you")) return 0L
    return dfs("you", mutableSetOf())
}

private fun part2(input: List<String>): Long {
    // Parse the input to build adjacency list
    val graph = mutableMapOf<String, List<String>>()

    for (line in input) {
        if (line.isBlank()) continue
        val parts = line.split(": ")
        if (parts.size != 2) continue

        val node = parts[0].trim()
        val neighbors = parts[1].split(" ").filter { it.isNotBlank() }
        graph[node] = neighbors
    }

    // DP approach: count paths using topological ordering
    fun countPathsDP(start: String, end: String, forbidden: Set<String> = emptySet()): Long {
        // dp[node] = number of paths from start to node
        val dp = mutableMapOf<String, Long>()
        dp[start] = 1L

        // BFS to process nodes in order
        val queue = ArrayDeque<String>()
        queue.add(start)
        val visited = mutableSetOf<String>()
        visited.add(start)

        // First pass: discover all reachable nodes
        val reachable = mutableSetOf<String>()
        val tempQueue = ArrayDeque<String>()
        tempQueue.add(start)
        val tempVisited = mutableSetOf<String>()
        tempVisited.add(start)

        while (tempQueue.isNotEmpty()) {
            val current = tempQueue.removeFirst()
            reachable.add(current)

            for (neighbor in graph[current] ?: emptyList()) {
                if (neighbor !in tempVisited && neighbor !in forbidden) {
                    tempVisited.add(neighbor)
                    tempQueue.add(neighbor)
                }
            }
        }

        // Process using memoization with DFS
        val memo = mutableMapOf<String, Long>()

        fun dfs(node: String): Long {
            if (node == end) return 1L
            if (node in memo) return memo[node]!!
            if (node in forbidden) return 0L

            var count = 0L
            for (neighbor in graph[node] ?: emptyList()) {
                if (neighbor !in forbidden) {
                    count += dfs(neighbor)
                }
            }

            memo[node] = count
            return count
        }

        return dfs(start)
    }

    // Calculate paths through both dac and fft in both orders
    val pathsDacFft = countPathsDP("svr", "dac") *
            countPathsDP("dac", "fft", setOf("svr")) *
            countPathsDP("fft", "out", setOf("svr", "dac"))

    val pathsFftDac = countPathsDP("svr", "fft") *
            countPathsDP("fft", "dac", setOf("svr")) *
            countPathsDP("dac", "out", setOf("svr", "fft"))

    return pathsDacFft + pathsFftDac
}
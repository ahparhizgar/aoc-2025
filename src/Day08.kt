fun main() {
    val testInput = readInput("Day08_test")
    val input = readInput("Day08")
    check(part1(testInput) == 20L)
    part1(input).println()

    check(part2(testInput) == 25272L)
    part2(input).println()
}

private fun part1(input: List<String>): Long {
    data class Point(val x: Long, val y: Long, val z: Long)
    data class Edge(val a: Int, val b: Int, val d2: Long)

    // parse input
    val points = input.mapNotNull { line ->
        val s = line.trim()
        if (s.isEmpty()) return@mapNotNull null
        val parts = s.split(',').map { it.trim() }
        if (parts.size != 3) return@mapNotNull null
        try {
            Point(parts[0].toLong(), parts[1].toLong(), parts[2].toLong())
        } catch (e: NumberFormatException) {
            null
        }
    }

    val n = points.size
    if (n == 0) return 0L

    // build all pairwise edges with squared distances
    val edges = ArrayList<Edge>((n * (n - 1)) / 2)
    for (i in 0 until n) {
        val pi = points[i]
        for (j in i + 1 until n) {
            val pj = points[j]
            val dx = pi.x - pj.x
            val dy = pi.y - pj.y
            val dz = pi.z - pj.z
            val d2 = dx * dx + dy * dy + dz * dz
            edges.add(Edge(i, j, d2))
        }
    }

    // sort edges by distance (squared)
    edges.sortWith(compareBy<Edge> { it.d2 })

    // union-find
    val parent = IntArray(n) { it }
    val size = IntArray(n) { 1 }

    fun find(a: Int): Int {
        var x = a
        while (parent[x] != x) {
            parent[x] = parent[parent[x]]
            x = parent[x]
        }
        return x
    }

    fun union(a: Int, b: Int) {
        var ra = find(a)
        var rb = find(b)
        if (ra == rb) return
        if (size[ra] < size[rb]) {
            val tmp = ra; ra = rb; rb = tmp
        }
        parent[rb] = ra
        size[ra] += size[rb]
    }

    val toProcess = minOf(1000, edges.size)
    for (k in 0 until toProcess) {
        val e = edges[k]
        union(e.a, e.b)
    }

    // compute component sizes
    val compCount = IntArray(n)
    for (i in 0 until n) {
        val r = find(i)
        compCount[r]++
    }
    val sizes = compCount.filter { it > 0 }.map { it.toLong() }.sortedDescending()

    // multiply top three sizes (fill with 1 if fewer than 3 components)
    var result = 1L
    for (i in 0 until 3) {
        result *= if (i < sizes.size) sizes[i] else 1L
    }
    return result
}

private fun part2(input: List<String>): Long {
    data class Point(val x: Long, val y: Long, val z: Long)
    data class Edge(val a: Int, val b: Int, val d2: Long)

    val points = input.mapNotNull { line ->
        val s = line.trim()
        if (s.isEmpty()) return@mapNotNull null
        val parts = s.split(',').map { it.trim() }
        if (parts.size != 3) return@mapNotNull null
        try {
            Point(parts[0].toLong(), parts[1].toLong(), parts[2].toLong())
        } catch (e: NumberFormatException) {
            null
        }
    }

    val n = points.size
    if (n == 0) return 0L
    if (n == 1) return points[0].x * points[0].x // trivial: last connection is self (edge case)

    // build all pairwise edges with squared distances
    val edges = ArrayList<Edge>((n * (n - 1)) / 2)
    for (i in 0 until n) {
        val pi = points[i]
        for (j in i + 1 until n) {
            val pj = points[j]
            val dx = pi.x - pj.x
            val dy = pi.y - pj.y
            val dz = pi.z - pj.z
            val d2 = dx * dx + dy * dy + dz * dz
            edges.add(Edge(i, j, d2))
        }
    }

    edges.sortWith(compareBy<Edge> { it.d2 })

    // union-find with merge check
    val parent = IntArray(n) { it }
    val size = IntArray(n) { 1 }

    fun find(a: Int): Int {
        var x = a
        while (parent[x] != x) {
            parent[x] = parent[parent[x]]
            x = parent[x]
        }
        return x
    }

    fun unionIfDifferent(a: Int, b: Int): Boolean {
        var ra = find(a)
        var rb = find(b)
        if (ra == rb) return false
        if (size[ra] < size[rb]) {
            val tmp = ra; ra = rb; rb = tmp
        }
        parent[rb] = ra
        size[ra] += size[rb]
        return true
    }

    var components = n
    for (e in edges) {
        if (unionIfDifferent(e.a, e.b)) {
            components--
            if (components == 1) {
                // the last connection that merged into a single component is e
                val x1 = points[e.a].x
                val x2 = points[e.b].x
                return x1 * x2
            }
        }
    }

    // Shouldn't happen for valid input, but return 0 if never unified
    return 0L
}
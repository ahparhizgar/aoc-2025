import kotlin.math.*

fun main() {
    val testInput = readInput("Day12_test")
    val input = readInput("Day12")

//    check(
        part1(testInput).println()// == 2L)
    part1(input).println()

//    check(part2(testInput) == 2L)
//    part2(input).println()
}

private data class Shape(val id: Int, val cells: List<Pair<Int, Int>>) {
    val area: Int = cells.size

    // Normalize cells to have min x,y = 0 and sort
    private fun norm(c: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
        val minx = c.minOf { it.first }
        val miny = c.minOf { it.second }
        return c.map { (x, y) -> (x - minx) to (y - miny) }.sortedWith(compareBy({ it.first }, { it.second }))
    }

    private fun rotate90(c: List<Pair<Int, Int>>): List<Pair<Int, Int>> =
        c.map { (x, y) -> (-y) to x }

    private fun flipX(c: List<Pair<Int, Int>>): List<Pair<Int, Int>> =
        c.map { (x, y) -> (-x) to y }

    fun orientations(): List<List<Pair<Int, Int>>> {
        val seen = HashSet<List<Pair<Int, Int>>>()
        var base = cells
        repeat(4) {
            val r = norm(base)
            if (seen.add(r)) {
                // also add flipped
                val f = norm(flipX(base))
                seen.add(f)
            }
            base = rotate90(base)
        }
        return seen.toList()
    }
}

private data class Region(val w: Int, val h: Int, val counts: IntArray)

/**
 * DLX with support for primary vs secondary columns.
 * - Primary columns: must be covered exactly once.
 * - Secondary columns: optional, but if covered, they must be covered at most once (conflict prevention).
 */
private class DLX(colCount: Int, rows: List<IntArray>, private val isPrimary: BooleanArray) {
    private data class Node(
        var up: Int, var down: Int,
        var left: Int, var right: Int,
        var col: Int, var rowId: Int
    )
    private val header = 0
    private val nodes = ArrayList<Node>()
    private val columnSize = IntArray(colCount + 1) // 1..colCount
    private val columnHead = IntArray(colCount + 1)

    init {
        // Create header node
        nodes.add(Node(0, 0, 0, 0, 0, -1))
        // Create column headers (not yet linked into primary list)
        for (c in 1..colCount) {
            val idx = nodes.size
            nodes.add(Node(idx, idx, 0, 0, c, -1)) // up=down=self
            columnHead[c] = idx
        }
        // Link only primary columns into header circular list
        var last = header
        for (c in 1..colCount) {
            if (!isPrimary[c]) continue
            val idx = columnHead[c]
            nodes[idx].left = last
            nodes[last].right = idx
            last = idx
        }
        nodes[last].right = header
        nodes[header].left = last

        // Build rows
        var rowIdx = 0
        for (row in rows) {
            var firstNode = -1
            var prevNode = -1
            for (c in row) {
                val colHead = columnHead[c]
                val idx = nodes.size
                // Insert into column bottom
                val colBottom = nodes[colHead].up
                nodes.add(
                    Node(
                        up = colBottom,
                        down = colHead,
                        left = 0,
                        right = 0,
                        col = c,
                        rowId = rowIdx
                    )
                )
                nodes[colBottom].down = idx
                nodes[colHead].up = idx
                columnSize[c]++

                // Link row circularly
                if (firstNode == -1) {
                    firstNode = idx
                    prevNode = idx
                    nodes[idx].left = idx
                    nodes[idx].right = idx
                } else {
                    nodes[idx].left = prevNode
                    nodes[idx].right = firstNode
                    nodes[prevNode].right = idx
                    nodes[firstNode].left = idx
                    prevNode = idx
                }
            }
            rowIdx++
        }
    }

    private fun cover(c: Int) {
        val colHead = columnHead[c]
        // If primary, unlink header
        if (isPrimary[c]) {
            nodes[nodes[colHead].left].right = nodes[colHead].right
            nodes[nodes[colHead].right].left = nodes[colHead].left
        }
        // Remove rows that contain this column
        var i = nodes[colHead].down
        while (i != colHead) {
            var j = nodes[i].right
            while (j != i) {
                val cc = nodes[j].col
                // unlink node j from its column
                val ch = columnHead[cc]
                nodes[nodes[j].down].up = nodes[j].up
                nodes[nodes[j].up].down = nodes[j].down
                columnSize[cc]--
                j = nodes[j].right
            }
            i = nodes[i].down
        }
    }

    private fun uncover(c: Int) {
        val colHead = columnHead[c]
        var i = nodes[colHead].up
        while (i != colHead) {
            var j = nodes[i].left
            while (j != i) {
                val cc = nodes[j].col
                columnSize[cc]++
                nodes[nodes[j].down].up = j
                nodes[nodes[j].up].down = j
                j = nodes[j].left
            }
            i = nodes[i].up
        }
        if (isPrimary[c]) {
            nodes[nodes[colHead].left].right = colHead
            nodes[nodes[colHead].right].left = colHead
        }
    }

    fun solveExists(limitSteps: Long = Long.MAX_VALUE): Boolean {
        var steps = 0L
        fun search(): Boolean {
            // If no remaining primary columns, solution found
            if (nodes[header].right == header) return true

            // choose primary column with smallest size
            var cNode = nodes[header].right
            var bestCol = nodes[cNode].col
            var bestSize = columnSize[bestCol]
            var walker = nodes[cNode].right
            while (walker != header) {
                val c = nodes[walker].col
                val size = columnSize[c]
                if (size < bestSize) {
                    bestSize = size
                    bestCol = c
                    cNode = walker
                    if (bestSize <= 1) break
                }
                walker = nodes[walker].right
            }
            if (bestSize == 0) return false

            cover(bestCol)
            var r = nodes[cNode].down
            while (r != cNode) {
                steps++
                if (steps > limitSteps) {
                    uncover(bestCol)
                    return false
                }
                // cover all columns in the chosen row (primary and secondary)
                var j = nodes[r].right
                while (j != r) {
                    cover(nodes[j].col)
                    j = nodes[j].right
                }
                if (search()) return true
                // backtrack
                j = nodes[r].left
                while (j != r) {
                    uncover(nodes[j].col)
                    j = nodes[j].left
                }
                r = nodes[r].down
            }
            uncover(bestCol)
            return false
        }
        return search()
    }
}

private fun parseInput(input: List<String>): Pair<List<Shape>, List<Region>> {
    val shapes = mutableListOf<Shape>()
    val regions = mutableListOf<Region>()
    var i = 0

    fun isRegionLine(s: String): Boolean =
        s.trim().matches(Regex("""^\d+\s*x\s*\d+\s*:\s*.*$"""))

    // Parse shapes until we see a region line
    while (i < input.size) {
        val line = input[i].trimEnd()
        if (line.isEmpty()) { i++; continue }
        if (isRegionLine(line)) break

        if (line.endsWith(":")) {
            val idStr = line.substringBefore(":").trim()
            val id = idStr.toInt() // shape index
            i++

            // Consume subsequent rows made of only '.' and '#'
            val rows = mutableListOf<String>()
            while (i < input.size) {
                val s = input[i]
                if (s.isEmpty()) { i++; break }
                if (isRegionLine(s)) break
                // Shape grid lines are only '.' and '#'
                if (s.any { it != '.' && it != '#' }) break
                rows.add(s)
                i++
            }

            // Collect shape cells
            val cells = mutableListOf<Pair<Int, Int>>()
            for (y in rows.indices) {
                val r = rows[y]
                for (x in r.indices) {
                    if (r[x] == '#') cells.add(x to y)
                }
            }
            if (cells.isEmpty()) {
                // Allow empty shapes defensively (shouldn't happen)
                shapes.add(Shape(id, emptyList()))
            } else {
                val minx = cells.minOf { it.first }
                val miny = cells.minOf { it.second }
                val norm = cells.map { (x, y) -> (x - minx) to (y - miny) }
                    .sortedWith(compareBy({ it.first }, { it.second }))
                shapes.add(Shape(id, norm))
            }
        } else {
            // Non-empty line but not a shape header: skip
            i++
        }
    }

    // Parse regions
    while (i < input.size) {
        val raw = input[i]
        val line = raw.trim()
        i++
        if (line.isEmpty()) continue
        if (!isRegionLine(line)) continue

        val parts = line.split(":", limit = 2)
        val dims = parts[0].trim()
        val countsStr = parts.getOrNull(1)?.trim().orEmpty()

        val w = dims.substringBefore("x").trim().toInt()
        val h = dims.substringAfter("x").trim().toInt()

        val counts = if (countsStr.isEmpty()) {
            IntArray(shapes.size)
        } else {
            countsStr.split(Regex("""\s+""")).filter { it.isNotEmpty() }.map { it.toInt() }.toIntArray()
        }

        regions.add(Region(w, h, counts))
    }

    val byId = shapes.sortedBy { it.id }
    return byId to regions
}

private fun generatePlacementsForRegion(w: Int, h: Int, shape: Shape): List<IntArray> {
    val rows = mutableListOf<IntArray>()
    val orients = shape.orientations()
    val orientBounds = orients.map { orient ->
        val maxx = orient.maxOf { it.first }
        val maxy = orient.maxOf { it.second }
        (maxx + 1) to (maxy + 1)
    }
    for (oi in orients.indices) {
        val orient = orients[oi]
        val (ow, oh) = orientBounds[oi]
        if (ow > w || oh > h) continue
        val offsets = orient.map { (x, y) -> y to x }
        for (y in 0..(h - oh)) {
            for (x in 0..(w - ow)) {
                val cols = IntArray(offsets.size) { k ->
                    val (dy, dx) = offsets[k]
                    val cx = x + dx
                    val cy = y + dy
                    cy * w + cx // 0-based cell index
                }
                rows.add(cols.sortedArray())
            }
        }
    }
    return rows
}

/**
 * Build matrix:
 * - Columns 1..(w*h) => secondary (cells, optional, prevent overlap).
 * - Columns (w*h+1).. => primary (piece slots, must be covered exactly once).
 */
private fun buildExactCoverMatrix(w: Int, h: Int, shapes: List<Shape>, counts: IntArray): Triple<Int, BooleanArray, List<IntArray>> {
    val totalCells = w * h
    val slotColStart = IntArray(shapes.size) { -1 }
    var colOffset = totalCells
    for (sIdx in shapes.indices) {
        val k = if (sIdx < counts.size) counts[sIdx] else 0
        if (k > 0) {
            slotColStart[sIdx] = colOffset
            colOffset += k
        }
    }
    val colCount = colOffset
    val isPrimary = BooleanArray(colCount + 1) { false }
    // mark piece slots as primary
    for (sIdx in shapes.indices) {
        val need = if (sIdx < counts.size) counts[sIdx] else 0
        if (need == 0) continue
        val start = slotColStart[sIdx]
        for (k in 0 until need) {
            isPrimary[start + k + 1] = true
        }
    }
    // cells (1..totalCells) remain secondary

    val allRows = mutableListOf<IntArray>()
    for (sIdx in shapes.indices) {
        val need = if (sIdx < counts.size) counts[sIdx] else 0
        if (need == 0) continue
        val placements = generatePlacementsForRegion(w, h, shapes[sIdx])
        val baseColsForPlacement = placements.map { cellCols ->
            IntArray(cellCols.size) { i -> cellCols[i] + 1 } // to 1-based
        }
        val start = slotColStart[sIdx]
        for (placementCols in baseColsForPlacement) {
            for (slotIdx in 0 until need) {
                val slotCol = start + slotIdx + 1
                val row = IntArray(placementCols.size + 1)
                for (i in placementCols.indices) row[i] = placementCols[i]
                row[placementCols.size] = slotCol
                row.sort()
                allRows.add(row)
            }
        }
    }
    return Triple(colCount, isPrimary, allRows)
}

private fun quickImpossible(w: Int, h: Int, shapes: List<Shape>, counts: IntArray): Boolean {
    val totalCells = w * h
    var needArea = 0
    for (i in shapes.indices) {
        val c = if (i < counts.size) counts[i] else 0
        if (c > 0) needArea += c * shapes[i].area
    }
    // Must fit within the region; can leave empty cells
    if (needArea > totalCells) return true
    return false
}

private fun canTileRegion(w: Int, h: Int, shapes: List<Shape>, counts: IntArray): Boolean {
    if (quickImpossible(w, h, shapes, counts)) return false
    val (colCount, isPrimary, rows) = buildExactCoverMatrix(w, h, shapes, counts)
    if (rows.isEmpty()) {
        // If no rows but counts require pieces, impossible; if counts all zero, trivially true
        val anyNeeded = counts.any { it > 0 }
        return !anyNeeded
    }
    val dlx = DLX(colCount, rows, isPrimary)
    return dlx.solveExists()
}

private fun solve(input: List<String>): Long {
    val (shapes, regions) = parseInput(input)
    var count = 0L
    for (r in regions) {
        if (canTileRegion(r.w, r.h, shapes, r.counts)) count++
    }
    return count
}

private fun part1(input: List<String>): Long {
    return solve(input)
}

private fun part2(input: List<String>): Long {
    TODO()
}

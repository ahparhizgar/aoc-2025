fun main() {
    val testInput = readInput("Day09_test")
    val input = readInput("Day09")

    // 4596179031 not an answer

    check(part1(testInput) == 50L)
    part1(input).println()

    check(part2(testInput) == 24L)
    part2(input).println()
}

private fun part1(input: List<String>): Long {
    return solution(input = input, requiresToBeInside = false)
}

private fun part2(input: List<String>): Long {
    return solution(input = input, requiresToBeInside = true)
}

private fun solution(input: List<String>, requiresToBeInside: Boolean): Long {
    val tiles = input.map { line ->
        val (x, y) = line.split(",").map { it.toInt() }
        Point(x, y)
    }

    if (tiles.size < 2) return 0L

    var maxArea = 0L

    // Try all pairs of tiles as opposite corners
    for (i in tiles.indices) {
        for (j in i + 1 until tiles.size) {
            val (x1, y1) = tiles[i]
            val (x2, y2) = tiles[j]

            // Calculate the area of the rectangle with these opposite corners
            // Include both endpoints (hence +1)
            val width = (Math.abs(x2 - x1) + 1).toLong()
            val height = (Math.abs(y2 - y1) + 1).toLong()
            val area = width * height
            if (!requiresToBeInside || isRectangleInsidePolyline(
                    tiles,
                    Point(x1, y1),
                    Point(x2, y2)
                )
            ) {
                maxArea = Math.max(maxArea, area)
                if (maxArea == 30L)
                    println("$x1, $y1, $x2, $y2")
            }
        }
    }

    return maxArea
}

data class Segment(val a: Point, val b: Point) {
    val isVertical = a.x == b.x
    val isHorizontal = a.y == b.y

    val minX = minOf(a.x, b.x)
    val maxX = maxOf(a.x, b.x)
    val minY = minOf(a.y, b.y)
    val maxY = maxOf(a.y, b.y)
}

enum class PointState {
    INSIDE,
    OUTSIDE,
    ON_EDGE
}

fun classifyPoint(p: Point, poly: List<Point>): PointState {

    var crossings = 0

    val polyEdges = buildList {
        for (i in 0 until poly.size - 1) {
            add(Segment(poly[i], poly[i + 1]))
        }
        add(Segment(poly.last(), poly.first())) // ✅ CLOSE the loop manually
    }
    for (seg in polyEdges) {
        // ✅ ON EDGE
        if (seg.isVertical &&
            p.x == seg.a.x &&
            p.y in seg.minY..seg.maxY
        ) {
            return PointState.ON_EDGE
        }

        if (seg.isHorizontal &&
            p.y == seg.a.y &&
            p.x in seg.minX..seg.maxX
        ) {
            return PointState.ON_EDGE
        }

        // ✅ Ray cast to the right (only vertical edges matter)
        if (seg.isVertical) {
            if (seg.a.x > p.x && p.y >= seg.minY && p.y < seg.maxY) {
                crossings++
            }
        }
    }

    return if (crossings % 2 == 1)
        PointState.INSIDE
    else
        PointState.OUTSIDE
}

fun areProperlyCrossing(a: Segment, b: Segment): Boolean {

    if (a.isVertical && b.isHorizontal) {
        return a.a.x in b.minX closed b.maxX &&
                b.a.y in a.minY closed a.maxY
    }

    if (a.isHorizontal && b.isVertical) {
        return b.a.x in a.minX closed a.maxX &&
                a.a.y in b.minY closed b.maxY
    }

    return false // parallel or touching: allowed
}

infix fun Int.closed(other: Int): ClosedRange {
    return ClosedRange(this, other)
}

class ClosedRange(val first: Int, val last: Int) {
    operator fun contains(value: Int): Boolean {
        return value < first && value > last
    }
}

fun isRectangleInsidePolyline(
    poly: List<Point>,   // closed: first == last
    a: Point,            // rectangle corner 1
    b: Point             // rectangle opposite corner
): Boolean {

    val minX = minOf(a.x, b.x)
    val maxX = maxOf(a.x, b.x)
    val minY = minOf(a.y, b.y)
    val maxY = maxOf(a.y, b.y)

    val rectPoints = listOf(
        Point(minX, minY),
        Point(maxX, minY),
        Point(maxX, maxY),
        Point(minX, maxY)
    )

    val rectEdges = listOf(
        Segment(rectPoints[0], rectPoints[1]),
        Segment(rectPoints[1], rectPoints[2]),
        Segment(rectPoints[2], rectPoints[3]),
        Segment(rectPoints[3], rectPoints[0])
    )

    val polyEdges = buildList {
        for (i in 0 until poly.size - 1) {
            add(Segment(poly[i], poly[i + 1]))
        }
        add(Segment(poly.last(), poly.first())) // ✅ CLOSE the loop manually
    }

    // ✅ Condition A: all rectangle corners inside or on polyline
    if (rectPoints.any { classifyPoint(it, poly) == PointState.OUTSIDE })
        return false

    // ✅ Condition B: no proper edge crossing
    for (r in rectEdges) {
        for (p in polyEdges) {
            if (areProperlyCrossing(r, p)) {
                return false
            }
        }
    }

    // ✅ Condition C: at least one strictly inside point
    val cx = (minX + maxX) / 2
    val cy = (minY + maxY) / 2
    val centerState = classifyPoint(Point(cx, cy), poly)

    return centerState == PointState.INSIDE
}

data class Point(val x: Int, val y: Int)

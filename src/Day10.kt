import java.util.PriorityQueue
import kotlin.math.max
import kotlin.math.min

fun main() {
    val testInput = readInput("Day10_test")
    val input = readInput("Day10")
    check(part1(testInput) == 7L)
    part1(input).println()

    check(part2(testInput) == 33L)
    part2(input).println()
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

        // BFS on state space of n lights (bitmask in [0 .. 2^n - 1])
        val dist = HashMap<Long, Int>(1 shl minOf(n, 20))
        val q: ArrayDeque<Long> = ArrayDeque()
        dist[0L] = 0
        q.add(0L)

        var found: Int? = null
        while (q.isNotEmpty()) {
            val s = q.removeFirst()
            val d = dist[s]!!
            if (s == target) {
                found = d
                break
            }
            for (b in uniqueButtons) {
                val ns = s xor b
                if (dist.putIfAbsent(ns, d + 1) == null) {
                    q.add(ns)
                }
            }
        }

        total += found?.toLong() ?: error("Target state unreachable for line: $line")
    }

    return total
}

private fun part2(input: List<String>): Long {
    var total = 0L

    val buttonRegex = Regex("\\(([^)]*)\\)")
    val curlyRegex = Regex("\\{([^}]*)\\}")

    for (line in input) {
        if (line.isBlank()) continue

        // Parse targets {a,b,c,...}
        val cm = curlyRegex.find(line) ?: error("No joltage requirements found in line: $line")
        val targetsRaw = cm.groupValues[1]
            .split(',')
            .mapNotNull { s -> s.trim().takeIf { it.isNotEmpty() }?.toInt() }
            .toIntArray()
        val mAll = targetsRaw.size

        // Early exit
        if (targetsRaw.all { it == 0 }) {
            continue
        }

        // Parse buttons as lists of indices (0-based)
        val buttonsRaw = buttonRegex.findAll(line).mapNotNull { m ->
            val inner = m.groupValues[1].trim()
            if (inner.isEmpty()) null
            else {
                inner.split(',').mapNotNull { tok ->
                    val s = tok.trim()
                    if (s.isEmpty()) null
                    else s.toInt()
                }.sorted().toIntArray()
            }
        }.toList()

        // Keep only counters with positive targets
        val keepIndices = ArrayList<Int>()
        for (i in 0 until mAll) if (targetsRaw[i] > 0) keepIndices.add(i)
        val m = keepIndices.size
        if (m == 0) {
            continue
        }
        val indexMap = HashMap<Int, Int>(mAll * 2)
        keepIndices.forEachIndexed { newIdx, oldIdx -> indexMap[oldIdx] = newIdx }

        val targets = IntArray(m) { idx -> targetsRaw[keepIndices[idx]] }

        // Filter and deduplicate buttons:
        // - Drop any button that touches a zero-target counter (can't press it).
        // - Map remaining indices to compact coordinates.
        // - Dedup by mask (same subset -> same variable).
        data class Button(val idxs: IntArray, val size: Int)
        val maskToIdxs = LinkedHashMap<Long, IntArray>()
        outer@ for (b in buttonsRaw) {
            if (b.isEmpty()) continue
            var mask = 0L
            var size = 0
            val mapped = ArrayList<Int>(b.size)
            for (old in b) {
                val newIdx = indexMap[old] ?: run {
                    // This button touches a zero-target counter -> unusable
                    continue@outer
                }
                if (mapped.isEmpty() || mapped.last() != newIdx) {
                    // Dedup duplicates within a button
                    mapped.add(newIdx)
                    size++
                    mask = mask or (1L shl newIdx)
                }
            }
            if (mask == 0L) continue
            // Deduplicate by mask
            maskToIdxs.putIfAbsent(mask, mapped.toIntArray())
        }
        val buttons = maskToIdxs.values.map { arr -> Button(arr, arr.size) }
        if (buttons.isEmpty()) {
            error("No usable buttons for non-zero targets in line: $line")
        }

        // Coverage check: every counter must be covered by at least one button
        val coverLists: Array<MutableList<Int>> = Array(m) { mutableListOf() }
        buttons.forEachIndexed { bi, b -> b.idxs.forEach { i -> coverLists[i].add(bi) } }
        for (i in 0 until m) {
            if (coverLists[i].isEmpty()) error("Target for counter $i is unreachable in line: $line")
        }

        // Precompute lower bound helpers
        val maxBtnSize = buttons.maxOf { it.size }.coerceAtLeast(1)

        // Pair-wise contribution caps k_ij = max over buttons of |{i,j} ∩ button|
        val kPairs = Array(m) { IntArray(m) { 0 } }
        for (i in 0 until m) kPairs[i][i] = 1 // trivial (never used)
        for ((_, b) in buttons.withIndex()) {
            // For each button, update kPairs
            val idxs = b.idxs
            // Singles (not needed; max(r) already accounts)
            // Pairs
            for (p in idxs.indices) {
                val i = idxs[p]
                // contribution 1 to (i, j) when only i present
                kPairs[i][i] = max(kPairs[i][i], 1)
                for (q in p + 1 until idxs.size) {
                    val j = idxs[q]
                    kPairs[i][j] = max(kPairs[i][j], 2)
                    kPairs[j][i] = kPairs[i][j]
                }
                // If button lacks j, pairs with j remain possibly 1, set below if not already 2
            }
        }
        // For pairs not covered jointly, but at least one singly covered, set to 1
        for (i in 0 until m) {
            val coveredI = coverLists[i].isNotEmpty()
            for (j in i + 1 until m) {
                val coveredJ = coverLists[j].isNotEmpty()
                if (kPairs[i][j] == 0 && (coveredI || coveredJ)) {
                    kPairs[i][j] = 1
                    kPairs[j][i] = 1
                }
            }
        }

        // Optional triples LB for small m
        val useTriples = m <= 12
        val triplesCap: HashMap<TripleKey, Int>? = if (useTriples) HashMap() else null

        if (useTriples) {
            // Key triple as IntArray of size 3 sorted; value = max |intersection|
            // Intersection size per press is 0..3.
            // Pre-fill with 0; update with buttons
            fun key(a: Int, b: Int, c: Int): TripleKey {
                // ensure sorted order
                var x = a; var y = b; var z = c
                if (x > y) { val t = x; x = y; y = t }
                if (y > z) { val t = y; y = z; z = t }
                if (x > y) { val t = x; x = y; y = t }
                return TripleKey(x, y, z)
            }
            for (i in 0 until m) {
                for (j in i + 1 until m) {
                    for (k in j + 1 until m) {
                        triplesCap!![key(i, j, k)] = 0
                    }
                }
            }
            for (b in buttons) {
                val s = b.idxs.toSet()
                for (i in 0 until m) {
                    for (j in i + 1 until m) {
                        for (k in j + 1 until m) {
                            var cnt = 0
                            if (i in s) cnt++
                            if (j in s) cnt++
                            if (k in s) cnt++
                            if (cnt > 0) {
                                val kk = key(i, j, k)
                                triplesCap!![kk] = max(triplesCap[kk]!!, cnt)
                            }
                        }
                    }
                }
            }
        }

        // Lower bound function (fast, strong)
        fun lowerBound(r: IntArray): Int {
            var sum = 0
            var mx = 0
            for (i in 0 until m) {
                val v = r[i]
                sum += v
                if (v > mx) mx = v
            }
            var lb = max(mx, (sum + maxBtnSize - 1) / maxBtnSize)

            // Pairs
            for (i in 0 until m) {
                val ri = r[i]
                if (ri == 0) continue
                for (j in i + 1 until m) {
                    val s = ri + r[j]
                    if (s == 0) continue
                    val cap = kPairs[i][j]
                    if (cap > 0) {
                        val need = (s + cap - 1) / cap
                        if (need > lb) lb = need
                    }
                }
            }

            if (useTriples && triplesCap != null) {
                for ((kKey, cap) in triplesCap) {
                    if (cap == 0) continue
                    val s3 = r[kKey.a] + r[kKey.b] + r[kKey.c]
                    if (s3 == 0) continue
                    val need = (s3 + cap - 1) / cap
                    if (need > lb) lb = need
                }
            }

            return lb
        }

        // Greedy feasible upper bound
        fun greedyUpperBound(): Int {
            val r = targets.copyOf()
            var presses = 0
            while (true) {
                var bestIdx = -1
                var bestScore = 0
                // find usable button with max coverage of remaining residual
                for (bi in buttons.indices) {
                    val b = buttons[bi]
                    var score = 0
                    var usable = true
                    for (i in b.idxs) {
                        if (r[i] == 0) { usable = false; break }
                        score += r[i]
                    }
                    if (!usable) continue
                    if (score > bestScore) {
                        bestScore = score
                        bestIdx = bi
                    }
                }
                if (bestIdx == -1) {
                    // No usable button; greedy cannot finish. Return a large upper bound
                    // so DFS can find a true feasible solution.
                    return Int.MAX_VALUE / 4
                }
                val b = buttons[bestIdx]
                var c = Int.MAX_VALUE
                for (i in b.idxs) c = min(c, r[i])
                if (c <= 0) continue
                for (i in b.idxs) r[i] -= c
                presses += c
                if (r.all { it == 0 }) break
            }
            return presses
        }

        // Memoization for pruning repeated residuals
        data class Vec(val v: IntArray) {
            override fun equals(other: Any?) = other is Vec && v.contentEquals(other.v)
            override fun hashCode(): Int = v.contentHashCode()
        }
        val bestSeen = HashMap<Vec, Int>(1024)

        // Apply forced moves until none apply.
        // Returns presses used, and records presses taken to 'applied' for backtracking.
        // If impossible under current residuals, returns Int.MAX_VALUE/4.
        fun applyForcedMoves(r: IntArray, applied: MutableList<Pair<Int, Int>>): Int {
            var added = 0
            while (true) {
                var progressed = false
                for (i in 0 until m) {
                    if (r[i] == 0) continue
                    // Count usable buttons covering i
                    var onlyIdx = -1
                    var count = 0
                    for (bi in coverLists[i]) {
                        val b = buttons[bi]
                        var usable = true
                        for (ii in b.idxs) {
                            if (r[ii] == 0) { usable = false; break }
                        }
                        if (usable) {
                            count++
                            if (count == 1) onlyIdx = bi
                            if (count > 1) break
                        }
                    }
                    if (count == 0) {
                        return Int.MAX_VALUE / 4 // impossible
                    }
                    if (count == 1) {
                        // Must press onlyIdx exactly r[i] times
                        val b = buttons[onlyIdx]
                        var cap = Int.MAX_VALUE
                        for (ii in b.idxs) cap = min(cap, r[ii])
                        val times = r[i]
                        if (times > cap) {
                            return Int.MAX_VALUE / 4
                        }
                        // Apply
                        for (ii in b.idxs) r[ii] -= times
                        applied.add(onlyIdx to times)
                        added += times
                        progressed = true
                        break // restart scanning since r changed
                    }
                }
                if (!progressed) break
            }
            return added
        }

        // Backtrack presses applied by applyForcedMoves
        fun revertForcedMoves(r: IntArray, applied: MutableList<Pair<Int, Int>>) {
            for (idx in applied.indices.reversed()) {
                val (bi, times) = applied[idx]
                val b = buttons[bi]
                for (i in b.idxs) r[i] += times
            }
            applied.clear()
        }

        // Apply and revert a specific press times of a button
        fun applyPress(r: IntArray, bi: Int, times: Int) {
            val b = buttons[bi]
            for (i in b.idxs) r[i] -= times
        }
        fun revertPress(r: IntArray, bi: Int, times: Int) {
            val b = buttons[bi]
            for (i in b.idxs) r[i] += times
        }

        // Initial upper bound (feasible)
        var best = greedyUpperBound()

        val r0 = targets.copyOf()

        fun dfs(r: IntArray, g: Int) {
            // Prune by lower bound
            val lb = lowerBound(r)
            if (g + lb >= best) return

            // Forced moves
            val forced = mutableListOf<Pair<Int, Int>>()
            val forcedCost = applyForcedMoves(r, forced)
            if (forcedCost >= Int.MAX_VALUE / 4) {
                revertForcedMoves(r, forced)
                return
            }
            val g2 = g + forcedCost
            if (g2 >= best) {
                revertForcedMoves(r, forced)
                return
            }
            if (r.all { it == 0 }) {
                if (g2 < best) best = g2
                revertForcedMoves(r, forced)
                return
            }

            // Memoization after forced normalization
            val key = Vec(r)
            val prev = bestSeen[key]
            if (prev != null && prev <= g2) {
                revertForcedMoves(r, forced)
                return
            }
            bestSeen[key] = g2

            // Choose pivot counter with largest residual
            var pivot = 0
            var pv = -1
            for (i in 0 until m) if (r[i] > pv) { pv = r[i]; pivot = i }

            // Candidate buttons: those that cover pivot and are usable (all indices > 0)
            val cands = mutableListOf<Int>()
            for (bi in coverLists[pivot]) {
                val b = buttons[bi]
                var usable = true
                for (ii in b.idxs) if (r[ii] == 0) { usable = false; break }
                if (usable) cands.add(bi)
            }
            if (cands.isEmpty()) {
                revertForcedMoves(r, forced)
                return
            }

            // Order candidates by current benefit descending
            cands.sortByDescending { bi ->
                var sc = 0
                for (ii in buttons[bi].idxs) sc += r[ii]
                sc
            }

            // Branch: for each candidate button, try pressing from max possible down to 1
            for (bi in cands) {
                val b = buttons[bi]
                var cap = Int.MAX_VALUE
                for (ii in b.idxs) cap = min(cap, r[ii])
                var t = cap
                while (t >= 1) {
                    applyPress(r, bi, t)
                    dfs(r, g2 + t)
                    revertPress(r, bi, t)
                    // quick bound to avoid hopeless tiny steps
                    if (g2 + t >= best) break
                    t--
                }
                // Optional: early exit if best equals lowerBound(targets) (can't do better)
                if (best == lowerBound(targets)) break
            }

            revertForcedMoves(r, forced)
        }

        dfs(r0, 0)

        total += best
    }

    return total
}
data class TripleKey(val a: Int, val b: Int, val c: Int)

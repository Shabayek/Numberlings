package com.vividoai.numberlings.core.engine

import com.vividoai.numberlings.core.model.Choice
import com.vividoai.numberlings.core.model.SkillKind
import com.vividoai.numberlings.core.model.ShapeKind
import com.vividoai.numberlings.core.model.Task
import com.vividoai.numberlings.core.model.TaskVisual
import com.vividoai.numberlings.core.model.VisualKind
import kotlin.random.Random

/**
 * Pure, handwritten puzzle factory. Deterministic: same inputs → same puzzle, so
 * review is reproducible and easy to test. Randomness is seeded, never "gacha".
 *
 * Difficulty tier: 0 = gentlest numbers, 2 = confident end of the skill.
 */
object TaskFactory {

    fun make(skill: SkillKind, tier: Int, seed: Long): Task = when (skill) {
        SkillKind.COUNT -> count(tier, seed)
        SkillKind.COMPARE -> compare(tier, seed)
        SkillKind.ADD_JOIN -> addJoin(tier, seed)
        SkillKind.ADD_MISSING -> addMissing(tier, seed)
        SkillKind.SUB_TAKE -> subTake(tier, seed)
        SkillKind.SUB_DIFFERENCE -> subDifference(tier, seed)
        SkillKind.SKIP_COUNT -> skipCount(tier, seed)
        SkillKind.TIMES_GROUPS -> timesGroups(tier, seed)
        SkillKind.SHARE_EVEN -> shareEven(tier, seed)
        SkillKind.SHARE_LEFTOVERS -> shareLeftovers(tier, seed)
        SkillKind.PARTITION -> partition(tier, seed)
        SkillKind.SHAPE_HUNT -> shapeHunt(seed)
    }

    // ---- helpers -------------------------------------------------------------

    private fun rnd(seed: Long) = Random(seed)

    /** Answer with close-by numeric distractors: distinct, ≥ 2 options, correct always present. */
    internal fun numericChoices(seed: Long, answer: Int, min: Int = 0, size: Int = 4): List<Choice> {
        val options = linkedSetOf(answer)
        val r = rnd(seed)
        var step = 1
        while (options.size < size && step <= 20) {
            val candidate = if (r.nextBoolean()) answer + step else answer - step
            if (candidate >= min) options.add(candidate)
            step++
        }
        // Safety net for very small numbers: fill with distinct in-range values.
        var fill = maxOf(0, min)
        while (options.size < 2) {
            options.add(answer + fill * 2)
            fill++
        }
        val list = options.filter { it >= min }
            .distinct()
            .shuffled(rnd(seed + 1))
            .toList()
        return list.map { Choice(id = "n$it", label = it.toString()) }
    }

    // ---- meadow 1: counting --------------------------------------------------

    private fun count(tier: Int, seed: Long): Task {
        val n = rnd(seed).nextUBTo(top = intArrayOf(6, 10, 20)[tier.coerceIn(0, 2)])
        val task = Task(
            skill = SkillKind.COUNT, tier = tier,
            prompt = "Count what you see, then tap the number.",
            visual = TaskVisual(kind = VisualKind.OBJECTS, left = n),
            choices = numericChoices(seed, n, min = 1),
            correctIndex = -1,
            spoken = "Count together $n little things. Tap each one, then choose the number."
        )
        return task.correct(n.toString())
    }

    private fun compare(tier: Int, seed: Long): Task {
        val r = rnd(seed)
        val askFewer = tier >= 2 && r.nextBoolean()
        val a = r.nextIntFrom(2, cap = 12)
        var b = r.nextIntFrom(1, cap = 13)
        if (b == a) b = a + if (r.nextBoolean()) 1 else -2
        b = b.coerceAtLeast(1)
        // No label buttons here: the child taps the group they can see.
        // Top group holds `a`, bottom group holds `b`.
        val answerIsTop = if (askFewer) a < b else a > b
        return Task(
            skill = SkillKind.COMPARE, tier = tier,
            prompt = "Tap the group with ${if (askFewer) "fewer" else "more"}.",
            visual = TaskVisual(kind = VisualKind.DIFFERENCE, left = a, right = b),
            choices = listOf(Choice("top", "Top group"), Choice("bottom", "Bottom group")),
            correctIndex = if (answerIsTop) 0 else 1,
            spoken = "Two groups of things. Tap the one with ${if (askFewer) "fewer" else "more"}.",
            tapTheGroups = true
        )
    }

    // ---- meadow 2: addition --------------------------------------------------

    private fun addJoin(tier: Int, seed: Long): Task {
        val r = rnd(seed)
        val maxSum = intArrayOf(8, 12, 20)[tier.coerceIn(0, 2)]
        val a = r.nextIntFrom(1, maxSum / 2)
        // Cap stays open-ended: a=1 must still allow b ≥ 1 without an empty range.
        val b = r.nextIntFrom(1, maxSum - a + 1)
        val sum = a + b
        return Task(
            skill = SkillKind.ADD_JOIN, tier = tier,
            prompt = "$a + $b = ?",
            visual = TaskVisual(kind = VisualKind.JOIN, left = a, right = b),
            choices = numericChoices(seed, sum, min = 1),
            correctIndex = -1,
            spoken = "$a little bees joined $b more bees. How many bees are playing now?"
        ).correct(sum.toString())
    }

    private fun addMissing(tier: Int, seed: Long): Task {
        val r = rnd(seed)
        val total = intArrayOf(10, 12, 20)[tier.coerceIn(0, 2)]
        val a = r.nextIntFrom(1, total - 1)
        val missing = total - a
        return Task(
            skill = SkillKind.ADD_MISSING, tier = tier,
            prompt = "$a + ? = $total",
            visual = TaskVisual(kind = VisualKind.JOIN, left = a, right = missing),
            choices = numericChoices(seed, missing, min = 1),
            correctIndex = -1,
            spoken = "$a and some more makes $total all together. How many more are needed?"
        ).correct(missing.toString())
    }

    // ---- meadow 3: subtraction ----------------------------------------------

    private fun subTake(tier: Int, seed: Long): Task {
        val r = rnd(seed)
        val total = intArrayOf(6, 10, 14)[tier.coerceIn(0, 2)]
        val taken = r.nextIntFrom(1, total - 2)
        val left = total - taken
        return Task(
            skill = SkillKind.SUB_TAKE, tier = tier,
            prompt = "$total − $taken = ?",
            visual = TaskVisual(kind = VisualKind.TAKE_AWAY, left = total, right = taken),
            choices = numericChoices(seed, left, min = 0),
            correctIndex = -1,
            spoken = "There were $total treats in the basket. $taken tumbled away. How many treats are left?"
        ).correct(left.toString())
    }

    private fun subDifference(tier: Int, seed: Long): Task {
        val r = rnd(seed)
        val cap = intArrayOf(6, 10, 14)[tier.coerceIn(0, 2)]
        val a = r.nextIntFrom(3, cap)
        val b = r.nextIntFrom(1, a - 1)
        val diff = a - b
        return Task(
            skill = SkillKind.SUB_DIFFERENCE, tier = tier,
            prompt = "How many more is $a than $b?",
            visual = TaskVisual(kind = VisualKind.DIFFERENCE, left = a, right = b),
            choices = numericChoices(seed, diff, min = 1),
            correctIndex = -1,
            spoken = "$a apples and $b pears. How many more apples are there?"
        ).correct(diff.toString())
    }

    // ---- meadow 4: skip counting & times tables ------------------------------

    private fun skipCount(tier: Int, seed: Long): Task {
        val r = rnd(seed)
        val step = intArrayOf(2, 5, 10)[r.nextInt(0, if (tier == 0) 1 else 2)]
        val hops = r.nextIntFrom(2, 6)
        val landing = step * hops
        return Task(
            skill = SkillKind.SKIP_COUNT, tier = tier,
            prompt = "Skip count by $step — land after $hops hops.",
            visual = TaskVisual(kind = VisualKind.NUMBER_LINE, from = 0, hops = hops * step),
            choices = numericChoices(seed, landing, min = step),
            correctIndex = -1,
            spoken = "Hop like a frog, jumping by $step! After $hops big hops, where does the frog land?"
        ).correct(landing.toString())
    }

    private fun timesGroups(tier: Int, seed: Long): Task {
        val r = rnd(seed)
        val rows = if (tier >= 2 && r.nextBoolean()) r.choice(3, 4) else intArrayOf(2, 5, 10)[r.nextInt(0, if (tier == 0) 1 else 2)]
        val cols = r.nextIntFrom(2, 5)
        val answer = rows * cols
        return Task(
            skill = SkillKind.TIMES_GROUPS, tier = tier,
            prompt = "$rows × $cols = ?",
            visual = TaskVisual(kind = VisualKind.GROUPS_ARRAY, rows = rows, cols = cols),
            choices = numericChoices(seed, answer, min = 2),
            correctIndex = -1,
            spoken = "$rows picnic baskets with $cols little mushrooms in each. How many mushrooms all together?"
        ).correct(answer.toString())
    }

    // ---- meadow 5: sharing ---------------------------------------------------

    private fun shareEven(tier: Int, seed: Long): Task {
        val r = rnd(seed)
        val plates = intArrayOf(2, 3, 5)[tier.coerceIn(0, 2)]
        val each = r.nextIntFrom(1, 6)
        val things = plates * each
        return Task(
            skill = SkillKind.SHARE_EVEN, tier = tier,
            prompt = "Share $things cakes on $plates plates — cakes per plate?",
            visual = TaskVisual(kind = VisualKind.PLATES, left = things, parts = plates),
            choices = numericChoices(seed, each, min = 1),
            correctIndex = -1,
            spoken = "$things cakes and $plates hungry friends. How many cakes does each friend get if everyone gets a fair share?"
        ).correct(each.toString())
    }

    private fun shareLeftovers(tier: Int, seed: Long): Task {
        val r = rnd(seed)
        val plates = intArrayOf(2, 3, 4)[tier.coerceIn(0, 2)]
        val each = r.nextIntFrom(1, 5)
        val leftovers = if (plates == 2) 1 else r.nextIntFrom(1, plates - 1)
        val things = plates * each + leftovers
        return Task(
            skill = SkillKind.SHARE_LEFTOVERS, tier = tier,
            prompt = "$things berries, $plates friends — berries left over?",
            visual = TaskVisual(kind = VisualKind.PLATES, left = things, parts = plates),
            choices = numericChoices(seed, leftovers, min = 1),
            correctIndex = -1,
            spoken = "$things berries and $plates plates. Give every plate its fair share first; then, how many berries are left over?"
        ).correct(leftovers.toString())
    }

    // ---- meadow 6: shapes & pizza parts --------------------------------------

    private fun partition(tier: Int, seed: Long): Task {
        val r = rnd(seed)
        val outOf = intArrayOf(4, 6, 8)[tier.coerceIn(0, 2)]
        val askQuarters = tier >= 2 && outOf == 8 && r.nextBoolean()
        val answer = if (askQuarters) outOf / 4 else outOf / 2
        val word = if (askQuarters) "one quarter" else "one half"
        return Task(
            skill = SkillKind.PARTITION, tier = tier,
            prompt = "A pizza has $outOf slices. How many slices is $word?",
            visual = TaskVisual(kind = VisualKind.PIZZA, parts = 2, outOf = outOf),
            choices = numericChoices(seed, answer, min = 1),
            correctIndex = -1,
            spoken = "One whole pizza, cut into $outOf slices. How many slices would you need to hold $word of it?"
        ).correct(answer.toString())
    }

    private fun shapeHunt(seed: Long): Task {
        val names = mapOf(
            ShapeKind.TRIANGLE to "Triangle",
            ShapeKind.RECTANGLE to "Rectangle",
            ShapeKind.CIRCLE to "Circle",
            ShapeKind.HEART to "Heart",
            ShapeKind.STAR to "Star"
        )
        val shape = names.keys.toList().shuffled(rnd(seed)).first()
        val r = rnd(seed + 2)
        val distractors = names.values.filter { it != names[shape] }.shuffled(r).take(3)
        val options = (distractors + names[shape]!!).shuffled(rnd(seed + 3))
        return Task(
            skill = SkillKind.SHAPE_HUNT, tier = 0,
            prompt = "Tap the name of this shape.",
            visual = TaskVisual(kind = VisualKind.SHAPES, shape = shape),
            choices = options.map { Choice(id = "s$it", label = it) },
            correctIndex = options.indexOf(names[shape]),
            spoken = "Look at the shape in the leaves. Tap what we call this kind of shape."
        )
    }

    private fun <T> Random.choice(vararg items: T): T = items[nextInt(items.size)]
    private fun Random.nextIntFrom(minInclusive: Int, cap: Int): Int = nextInt(minInclusive, cap)
    /** `cap` is exclusive, matching [kotlin.random.Random.nextInt]. */
    private fun Random.nextUBTo(top: Int): Int = nextInt(3, top + 1)

    private fun Task.correct(answerLabel: String): Task =
        copy(correctIndex = choices.indexOfFirst { it.label == answerLabel }).also {
            check(it.correctIndex >= 0) { "Answer '$answerLabel' missing from choices of $skill" }
        }
}

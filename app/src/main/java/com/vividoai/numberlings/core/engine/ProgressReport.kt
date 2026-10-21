package com.vividoai.numberlings.core.engine

import com.vividoai.numberlings.core.model.Meadow
import com.vividoai.numberlings.core.model.SkillKind
import com.vividoai.numberlings.core.model.StickerAlbum

/**
 * The report a grown-up shares with a teacher, a relative or a doctor.
 *
 * It has to be readable by an adult who has never seen the game: named
 * skills, plain words, a clear shape. A short machine-readable section is
 * kept at the end so nobody has to choose between friendly and useful.
 */
object ProgressReport {

    /** A plain name for every skill. */
    fun labelOf(skill: SkillKind): String = when (skill) {
        SkillKind.COUNT -> "Counting"
        SkillKind.COMPARE -> "Comparing groups"
        SkillKind.ADD_JOIN -> "Joining groups"
        SkillKind.ADD_MISSING -> "The missing number"
        SkillKind.SUB_TAKE -> "Taking away"
        SkillKind.SUB_DIFFERENCE -> "How many more"
        SkillKind.TIMES_GROUPS -> "Groups of times"
        SkillKind.SKIP_COUNT -> "Counting in steps"
        SkillKind.SHARE_EVEN -> "Sharing equally"
        SkillKind.SHARE_LEFTOVERS -> "What is left over"
        SkillKind.PARTITION -> "Halves and quarters"
        SkillKind.SHAPE_HUNT -> "Shapes"
    }

    /**
     * The whole report as plain text.
     *
     * @param masteryPercent how confident the child is with each skill, 0-100.
     * @param hearts hearts earned so far.
     * @param blocks puzzle blocks finished.
     * @param stickers the album slots collected.
     */
    fun render(
        masteryPercent: Map<SkillKind, Int>,
        hearts: Int,
        blocks: Int,
        stickers: Set<Int>,
        stickerTotal: Int = StickerAlbum.TOTAL
    ): String {
        val out = StringBuilder()

        out.appendLine("NUMBERLINGS - PROGRESS REPORT")
        out.appendLine("==============================")
        out.appendLine()
        out.appendLine("Hello! This is how far a young mathematician has come in Numberlings,")
        out.appendLine("a small offline maths game. Every number here comes from practising:")
        out.appendLine("nothing was bought, and nothing is sent anywhere except by you,")
        out.appendLine("right now, in this message.")
        out.appendLine()
        out.appendLine("AT A GLANCE")
        out.appendLine("-----------")
        out.appendLine("  Puzzle blocks finished .... $blocks")
        out.appendLine("  Hearts earned .............. $hearts")
        out.appendLine("  Stickers collected ......... ${stickers.size} of $stickerTotal")
        out.appendLine()
        out.appendLine("HOW THE SKILLS ARE GOING")
        out.appendLine("------------------------")
        out.appendLine("Each line is one skill, the meadow it lives in, and how confident")
        out.appendLine("the mathematician is with it (100% = knows it well, under 50% = still new).")
        out.appendLine()

        for (meadow in Meadow.entries) {
            val skills = Curriculum.meadowSkills[meadow].orEmpty().filter { it in masteryPercent }
            if (skills.isEmpty()) continue
            out.appendLine("  ${meadow.title} - ${meadow.subtitle}")
            for (skill in skills) {
                val percent = masteryPercent.getValue(skill)
                out.appendLine("      ${labelOf(skill).padEnd(24)} ${percent.toString().padStart(3)}%")
            }
            out.appendLine()
        }

        if (masteryPercent.isEmpty()) {
            out.appendLine("  No puzzles have been played yet.")
            out.appendLine()
        }

        if (stickers.isNotEmpty()) {
            out.appendLine("THE STICKER ALBUM")
            out.appendLine("------------------")
            out.appendLine("A sticker is earned at every Market Day, and a Market Day comes after")
            out.appendLine("every five finished blocks of puzzles (at the market, counting out")
            out.appendLine("the exact number of apples the stall keeper asks for).")
            out.appendLine("The album has 30 slots - one for every sticker that can be earned.")
            out.appendLine()
            out.appendLine(
                "  Filled so far: ${stickers.size} of $stickerTotal " +
                    "(${slotWord(stickers.size)} ${slotsAsWords(stickers)})."
            )
            out.appendLine()
            out.appendLine("  The album, slot by slot:")
            out.appendLine()
            albumStrip(stickers, stickerTotal).forEach { out.appendLine("    $it") }
            out.appendLine()
        }

        out.appendLine()
        out.appendLine("A snapshot of the practice so far. It was made on this device, and")
        out.appendLine("Numberlings keeps no copy of it anywhere else.")
        return out.toString().trimEnd() + "\n"
    }

    /** "5, 10 and 25" - a list a reader can say out loud. */
    private fun slotsAsWords(stickers: Set<Int>): String {
        val slots = stickers.sorted().map { it + 1 }
        return when (slots.size) {
            0 -> "none yet"
            1 -> slots[0].toString()
            2 -> "${slots[0]} and ${slots[1]}"
            else -> slots.dropLast(1).joinToString(", ") + " and " + slots.last()
        }
    }

    /** "slot" or "slots", depending on how many. */
    private fun slotWord(count: Int): String = if (count == 1) "slot" else "slots"

    /** The 30-slot album, drawn so the parent can see it at a glance. */
    private fun albumStrip(stickers: Set<Int>, stickerTotal: Int): List<String> {
        val perRow = 15
        return (0 until stickerTotal).chunked(perRow).map { row ->
            row.joinToString(" ") { slot ->
                if (slot in stickers) "[x]" else "[ ]"
            }
        }
    }
}

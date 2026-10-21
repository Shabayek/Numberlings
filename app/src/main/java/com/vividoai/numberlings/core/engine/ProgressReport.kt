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
        out.appendLine("THE SHORT VERSION")
        out.appendLine("-----------------")
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
            out.appendLine("STICKERS")
            out.appendLine("--------")
            out.appendLine(
                "  Collected slots: " +
                    stickers.sorted().joinToString(", ") { (it + 1).toString() } +
                    "  (of $stickerTotal)"
            )
            out.appendLine()
        }

        out.appendLine("FOR THE RECORD")
        out.appendLine("--------------")
        out.appendLine("skill_id,skill,mastery_percent")
        for (skill in SkillKind.entries) {
            val percent = masteryPercent[skill] ?: continue
            out.appendLine("${skill.ordinal},${labelOf(skill)},$percent")
        }
        out.appendLine("hearts,$hearts")
        out.appendLine("blocks_completed,$blocks")
        if (stickers.isNotEmpty()) {
            out.appendLine("sticker_slots,${stickers.sorted().joinToString(",") { (it + 1).toString() }}")
        }

        return out.toString().trimEnd() + "\n"
    }
}

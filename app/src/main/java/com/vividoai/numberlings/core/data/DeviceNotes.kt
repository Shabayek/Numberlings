package com.vividoai.numberlings.core.data

import android.content.Context
import com.vividoai.numberlings.core.model.Meadow

/**
 * Small local notes the app keeps beside the village notebook: how far the
 * current puzzle block has wandered, and when a grown-up last passed the
 * gate. Both are on this device only, and both exist so the app never has to
 * ask a child to do something twice.
 */
class DeviceNotes(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("numberlings_notes", Context.MODE_PRIVATE)

    // ---- the block in progress ------------------------------------------------

    /** Solved count and seed of the block in progress in [meadow], if any. */
    fun blockProgress(meadow: Meadow): Pair<Int, Long>? {
        if (prefs.getString(KEY_MEADOW, null) != meadow.name) return null
        val solved = prefs.getInt(KEY_SOLVED, 0)
        val seed = prefs.getLong(KEY_SEED, 0L)
        if (solved <= 0) return null
        return solved to seed
    }

    /** Remember how far this block has gone, so a restart never loses it. */
    fun saveBlockProgress(meadow: Meadow, solved: Int, seed: Long) {
        prefs.edit()
            .putString(KEY_MEADOW, meadow.name)
            .putInt(KEY_SOLVED, solved)
            .putLong(KEY_SEED, seed)
            .apply()
    }

    /** The block is finished: nothing left to remember. */
    fun clearBlockProgress(meadow: Meadow) {
        if (prefs.getString(KEY_MEADOW, null) != meadow.name) return
        prefs.edit().remove(KEY_MEADOW).remove(KEY_SOLVED).remove(KEY_SEED).apply()
    }

    // ---- grown-up gate grace --------------------------------------------------

    /** True when a grown-up passed the gate recently, so they need not redo it. */
    fun gateGraceActive(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val last = prefs.getLong(KEY_GATE_AT, 0L)
        if (last <= 0L) return false
        return nowMillis - last < GATE_GRACE_MILLIS
    }

    /** Remember that a grown-up passed the gate just now. */
    fun markGatePassed(nowMillis: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_GATE_AT, nowMillis).apply()
    }

    companion object {
        const val GATE_GRACE_MINUTES = 30
        const val GATE_GRACE_MILLIS = GATE_GRACE_MINUTES * 60_000L

        private const val KEY_MEADOW = "block_meadow"
        private const val KEY_SOLVED = "block_solved"
        private const val KEY_SEED = "block_seed"
        private const val KEY_GATE_AT = "gate_passed_at"
    }
}

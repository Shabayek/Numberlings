package com.vividoai.numberlings.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Grown-up controlled knobs. Defaults protect the child; each one is optional
 * and nothing here is a mandate: the child plays however they wish.
 */
data class GrownUps(
    val narration: Boolean = true,
    val sound: Boolean = true,
    val soundVolume: Int = 60,              // 0..100
    val voiceSpeed: Int = 9,                // 5..15 → rate 0.5..1.5 (read-aloud warmth)
    /** 0 = don't adjust; otherwise minutes before the gentle sleep invitation. */
    val sessionCapMinutes: Int = 12,
    /** -1..+1 puzzle difficulty nudge for accessibility (SkillScore.tier). */
    val bias: Int = 0
) {
    val rateFloat: Float get() = (0.5f + voiceSpeed * 0.1f)
}

private val Context.store: DataStore<Preferences> by preferencesDataStore(name = "grownups")

class GrownUpsStore(private val context: Context) {

    private object Keys {
        val narration = booleanPreferencesKey("narration")
        val sound = booleanPreferencesKey("sound")
        val volume = intPreferencesKey("volume")
        val voice = intPreferencesKey("voice_speed")
        val cap = intPreferencesKey("session_cap")
        val bias = intPreferencesKey("bias")
    }

    val flow: Flow<GrownUps> = context.store.data.map { p ->
        GrownUps(
            narration = p[Keys.narration] ?: true,
            sound = p[Keys.sound] ?: true,
            soundVolume = p[Keys.volume] ?: 60,
            voiceSpeed = p[Keys.voice] ?: 9,
            sessionCapMinutes = p[Keys.cap] ?: 12,
            bias = p[Keys.bias] ?: 0
        )
    }

    suspend fun save(next: GrownUps) {
        context.store.edit { p ->
            p[Keys.narration] = next.narration
            p[Keys.sound] = next.sound
            p[Keys.volume] = next.soundVolume
            p[Keys.voice] = next.voiceSpeed
            p[Keys.cap] = next.sessionCapMinutes
            p[Keys.bias] = next.bias
        }
    }
}

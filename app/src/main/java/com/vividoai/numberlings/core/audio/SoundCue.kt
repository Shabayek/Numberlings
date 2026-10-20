package com.vividoai.numberlings.core.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Tiny, warm sound cues. One synthesiser, no audio files, no licences,
 * and volumes the grown-ups control. Nothing auto-plays: only taps make sound.
 *
 * The synthesiser is rebuilt when the grown-up changes the level, because a
 * ToneGenerator keeps the volume it was born with.
 */
class SoundCue(context: Context) {

    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var volumePercent = 60
    private var generator: ToneGenerator? = null

    private fun ToneGenerator.play(tone: Int, ms: Int) {
        try {
            startTone(tone, ms)
        } catch (runaway: Exception) {
            // Sound is decoration; a missing audio device must never pause play.
        }
    }

    /** The live synthesiser — or none at all when the cues are switched off. */
    private fun currentTone(): ToneGenerator? {
        if (volumePercent <= 0) return null
        val existing = generator
        if (existing != null) return existing
        val made = try {
            ToneGenerator(AudioManager.STREAM_MUSIC, volumePercent)
        } catch (runaway: Exception) {
            null
        }
        generator = made
        return made
    }

    /** The grown-ups' sound level (0 = silent). Takes effect straight away. */
    fun setVolume(percent: Int) {
        val next = percent.coerceIn(0, 100)
        if (next == volumePercent) return
        volumePercent = next
        generator?.release()
        generator = null
    }

    fun release() {
        generator?.release()
        generator = null
    }

    fun tap() { currentTone()?.play(ToneGenerator.TONE_PROP_BEEP, 60) }
    fun yes() { currentTone()?.play(ToneGenerator.TONE_PROP_ACK, 150) }
    fun maybe() { currentTone()?.play(ToneGenerator.TONE_PROP_BEEP2, 120) }
    fun poke() { currentTone()?.play(ToneGenerator.TONE_CDMA_PIP, 90) }
}

package com.vividoai.numberlings.core.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Tiny, warm sound cues. One synthesiser, no audio files, no licences,
 * and volumes the grown-ups control. Nothing auto-plays: only taps make sound.
 */
class SoundCue(context: Context) {

    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var volumePercent = 60

    private fun ToneGenerator.play(tone: Int, ms: Int) {
        try {
            startTone(tone, ms)
        } catch (runaway: Exception) {
            // Sound is decoration; a missing audio device must never pause play.
        }
    }

    private val tone: ToneGenerator by lazy { ToneGenerator(manager.streamVolumeType(), volumePercent) }

    private fun AudioManager.streamVolumeType(): Int = AudioManager.STREAM_MUSIC

    fun setVolume(percent: Int) {
        volumePercent = percent.coerceIn(0, 100)
    }

    fun release() {
        tone.release()
    }

    fun tap() { if (volumePercent > 0) tone.play(ToneGenerator.TONE_PROP_BEEP, 60) }
    fun yes() { if (volumePercent > 0) tone.play(ToneGenerator.TONE_PROP_ACK, 150) }
    fun maybe() { if (volumePercent > 0) tone.play(ToneGenerator.TONE_PROP_BEEP2, 120) }
    fun poke() { if (volumePercent > 0) tone.play(ToneGenerator.TONE_CDMA_PIP, 90) }
}

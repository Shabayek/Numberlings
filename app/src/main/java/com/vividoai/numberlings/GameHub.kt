package com.vividoai.numberlings

import android.content.Context
import com.vividoai.numberlings.core.audio.SoundCue
import com.vividoai.numberlings.core.audio.Speaker
import com.vividoai.numberlings.core.data.GrownUps
import com.vividoai.numberlings.core.data.VillageRepository

/**
 * One small, honest service locator so screens can focus on play.
 * Everything it holds lives on this device; no network anywhere.
 */
object GameHub {
    lateinit var repository: VillageRepository
        private set
    lateinit var speaker: Speaker
        private set
    lateinit var sound: SoundCue
        private set

    fun wire(context: Context) {
        if (this::repository.isInitialized) return
        repository = VillageRepository(context)
        speaker = Speaker(context)
        sound = SoundCue(context)
    }

    fun apply(grownUps: GrownUps) {
        if (this::speaker.isInitialized) {
            speaker.rate = grownUps.rateFloat
        }
        if (this::sound.isInitialized) {
            sound.setVolume(if (grownUps.sound) grownUps.soundVolume else 0)
        }
    }

    fun shutter() {
        if (this::speaker.isInitialized) speaker.shutdown()
    }
}

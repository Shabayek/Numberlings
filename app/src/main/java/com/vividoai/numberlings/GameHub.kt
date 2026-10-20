package com.vividoai.numberlings

import android.content.Context
import com.vividoai.numberlings.core.audio.SoundCue
import com.vividoai.numberlings.core.audio.Speaker
import com.vividoai.numberlings.core.data.GrownUps
import com.vividoai.numberlings.core.data.VillageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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

    /** Audio settings are applied on the main thread, where the audio APIs live. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun wire(context: Context) {
        if (this::repository.isInitialized) return
        repository = VillageRepository(context)
        speaker = Speaker(context)
        sound = SoundCue(context)
        // The grown-ups' knobs reach the audio layer the moment they change.
        repository.settings.onEach { apply(it) }.launchIn(scope)
    }

    fun apply(grownUps: GrownUps) {
        // Audio is decoration: a missing engine or a refused sound device must
        // never interrupt the play.
        try {
            if (this::speaker.isInitialized) {
                speaker.rate = grownUps.rateFloat
            }
        } catch (failure: Exception) {
            android.util.Log.w("Numberlings", "voice settings not applied", failure)
        }
        try {
            if (this::sound.isInitialized) {
                sound.setVolume(if (grownUps.sound) grownUps.soundVolume else 0)
            }
        } catch (failure: Exception) {
            android.util.Log.w("Numberlings", "sound settings not applied", failure)
        }
    }
}

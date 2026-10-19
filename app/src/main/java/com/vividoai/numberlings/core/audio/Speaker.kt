package com.vividoai.numberlings.core.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Offline narration for pre-readers. Uses the system text-to-speech service —
 * no network calls are made by this app, and nothing is recorded or uploaded.
 * If the device has no TTS voice installed, the game stays fully playable;
 * narration is an enhancement, never a gate.
 */
class Speaker(context: Context) : TextToSpeech.OnInitListener {

    private var ready = false
    private val engine = TextToSpeech(context.applicationContext, this)

    var rate = 0.9f
        set(value) {
            field = value.coerceIn(0.5f, 1.5f)
            if (ready) engine.setSpeechRate(field)
        }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            engine.setLanguage(Locale.US)
            engine.setSpeechRate(rate)
            ready = true
        }
    }

    fun say(text: String) {
        if (!ready) return
        engine.stop()
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "puzzle")
    }

    fun hush() {
        if (ready) engine.stop()
    }

    fun shutdown() {
        if (ready) engine.stop()
        engine.shutdown()
    }
}

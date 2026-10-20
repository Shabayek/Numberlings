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

    /** The grown-up's chosen pace. 0.5x is the warmest, 1.5x the quickest. */
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

    fun say(text: String, slow: Boolean = false) {
        if (!ready) return
        if (slow) {
            // Bedtime voice: slowest, warmest reading, then restore the
            // grown-up's chosen pace a beat later.
            engine.setSpeechRate(0.55f)
        }
        engine.stop()
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "puzzle")
        if (slow) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (ready) engine.setSpeechRate(rate)
            }, 900)
        }
    }

    fun hush() {
        if (ready) engine.stop()
    }

    fun shutdown() {
        if (ready) engine.stop()
        engine.shutdown()
    }
}

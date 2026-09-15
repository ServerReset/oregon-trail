package com.oregontrail.app

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import com.oregontrail.engine.Sound

/** Plays the game's little tone cues and the matching haptics. */
class SoundPlayer {

    private val handler = Handler(Looper.getMainLooper())
    private var tone: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 60)
    } catch (_: Exception) {
        null
    }
    /** A little tune per event, played as a sequence of short tones. */
    private fun sequenceFor(s: Sound): List<Pair<Int, Int>> = when (s) {
        Sound.CLICK -> listOf(ToneGenerator.TONE_PROP_BEEP to 30)
        Sound.SELECT -> listOf(ToneGenerator.TONE_PROP_BEEP to 45)
        Sound.PAGE -> listOf(ToneGenerator.TONE_PROP_BEEP to 25)
        Sound.GOOD -> listOf(
            ToneGenerator.TONE_PROP_ACK to 80,
            ToneGenerator.TONE_PROP_BEEP to 80
        )
        Sound.BAD -> listOf(ToneGenerator.TONE_PROP_NACK to 170)
        Sound.SHOOT -> listOf(ToneGenerator.TONE_CDMA_PIP to 50)
        Sound.HIT -> listOf(
            ToneGenerator.TONE_CDMA_PIP to 40,
            ToneGenerator.TONE_PROP_ACK to 60
        )
        Sound.INJURY -> listOf(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD to 220)
        Sound.MILESTONE -> listOf(
            ToneGenerator.TONE_PROP_BEEP to 70,
            ToneGenerator.TONE_PROP_ACK to 90
        )
        Sound.RIVER -> listOf(
            ToneGenerator.TONE_CDMA_PIP to 40,
            ToneGenerator.TONE_CDMA_PIP to 40
        )
        Sound.TRADE -> listOf(
            ToneGenerator.TONE_PROP_BEEP to 55,
            ToneGenerator.TONE_PROP_BEEP to 55
        )
        Sound.REST -> listOf(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE to 190)
        Sound.DEATH -> listOf(
            ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD to 380,
            ToneGenerator.TONE_PROP_NACK to 260
        )
        Sound.ARRIVAL -> listOf(
            ToneGenerator.TONE_PROP_ACK to 130,
            ToneGenerator.TONE_PROP_BEEP to 130,
            ToneGenerator.TONE_PROP_ACK to 200
        )
        Sound.UNLOCK -> listOf(
            ToneGenerator.TONE_PROP_BEEP to 60,
            ToneGenerator.TONE_CDMA_PIP to 60,
            ToneGenerator.TONE_PROP_ACK to 130
        )
    }


private var soundToken = 0

    fun play(s: Sound) {
        val gen = tone ?: return
        val token = ++soundToken
        var delay = 0L
        for ((type, duration) in sequenceFor(s)) {
            handler.postDelayed({
                if (token != soundToken) return@postDelayed
                try {
                    gen.startTone(type, duration)
                } catch (_: Exception) {
                    // Ignore audio failures.
                }
            }, delay)
            delay += duration.toLong() + 35L
        }
    }


    /** A matching vibration for the important moments, when enabled. */
    fun haptic(view: View, s: Sound, enabled: Boolean) {
        if (!enabled) return
        val constant = when (s) {
            Sound.BAD, Sound.INJURY -> HapticFeedbackConstants.LONG_PRESS
            Sound.DEATH, Sound.ARRIVAL -> HapticFeedbackConstants.LONG_PRESS
            Sound.HIT, Sound.SHOOT -> HapticFeedbackConstants.KEYBOARD_TAP
            Sound.MILESTONE, Sound.UNLOCK -> HapticFeedbackConstants.CLOCK_TICK
            else -> return
        }
        try {
            view.performHapticFeedback(constant)
        } catch (_: Exception) {
            // Haptics are best-effort.
        }
    }


    fun release() {
        soundToken++
        tone?.release()
        tone = null
    }
}

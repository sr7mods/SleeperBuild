package com.sleeper.build7.audio

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Global Audio Engine for SleeperBuild.
 * Powers Custom Text-To-Speech (TTS) with Cute Girl / Cute Boy voice pitch profiles,
 * and standard System Ringtone alarm audio across Schedules, Prayer, Workout, etc.
 */
object SleeperAudioEngine {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var pendingSpeech: (() -> Unit)? = null
    private var activeRingtone: Ringtone? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun initialize(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.US)
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        isTtsReady = true
                        pendingSpeech?.invoke()
                        pendingSpeech = null
                    }
                }
            }
        }
    }

    /**
     * Speaks text using selected voice profile:
     * - Cute Girl: pitch ~1.35x, speechRate ~1.05x (bright, sweet, energetic)
     * - Cute Boy: pitch ~0.90x, speechRate ~1.00x (confident, slightly deeper, crisp)
     */
    fun speakTts(
        context: Context,
        text: String,
        voiceProfile: String = "girl",
        repeatCount: Int = 1
    ) {
        initialize(context)

        val speakAction: () -> Unit = {
            tts?.let { engine ->
                applyVoiceProfile(engine, voiceProfile)

                val utteranceId = "SleeperTTS_${System.currentTimeMillis()}"
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

                if (repeatCount > 1) {
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        private var played = 1
                        override fun onStart(id: String?) {}
                        override fun onDone(id: String?) {
                            if (played < repeatCount) {
                                played++
                                mainHandler.postDelayed({
                                    engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                                }, 1200L)
                            }
                        }
                        override fun onError(id: String?) {}
                    })
                }
            }
            Unit
        }

        if (isTtsReady) {
            speakAction()
        } else {
            pendingSpeech = speakAction
        }
    }

    private fun applyVoiceProfile(engine: TextToSpeech, profile: String) {
        when (profile.lowercase()) {
            "girl" -> {
                engine.setPitch(1.35f)
                engine.setSpeechRate(1.05f)
            }
            "boy" -> {
                engine.setPitch(0.90f)
                engine.setSpeechRate(1.00f)
            }
            else -> {
                engine.setPitch(1.0f)
                engine.setSpeechRate(1.0f)
            }
        }
    }

    /**
     * Plays default system alarm or notification ringtone.
     */
    fun playSystemRingtone(context: Context, durationMs: Long = 4000L) {
        stopAudio()
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            activeRingtone = RingtoneManager.getRingtone(context, ringtoneUri)
            activeRingtone?.play()

            mainHandler.postDelayed({
                stopAudio()
            }, durationMs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Global router for alarms & triggers across all modules.
     */
    fun playAlarmOrTts(
        context: Context,
        mode: String,
        customText: String,
        voiceProfile: String = "girl"
    ) {
        if (mode == "ringtone") {
            playSystemRingtone(context)
        } else {
            speakTts(context, customText, voiceProfile, repeatCount = 2)
        }
    }

    fun stopAudio() {
        try {
            activeRingtone?.stop()
            activeRingtone = null
        } catch (e: Exception) {
            // ignore
        }
        try {
            tts?.stop()
        } catch (e: Exception) {
            // ignore
        }
    }
}

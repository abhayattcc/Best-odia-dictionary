package com.abhayattcc.dictionaryreader.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object TtsUtils {
    private var tts: TextToSpeech? = null

    fun initialize(context: Context, onVoicesAvailable: (List<Voice>) -> Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val voices = tts?.voices?.toList() ?: emptyList()
                onVoicesAvailable(voices)
            }
        }
    }

    fun speak(text: String, language: String, onDone: () -> Unit) {
        val locale = when (language) {
            "en" -> Locale.ENGLISH
            "hi" -> Locale("hi", "IN")
            "or" -> Locale("or", "IN")
            else -> Locale.getDefault()
        }
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { onDone() }
            override fun onError(utteranceId: String?) {}
        })
    }

    fun stop() {
        tts?.stop()
    }
}
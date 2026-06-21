package com.giathinh.canlua.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class TtsHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private var textToSpeech: TextToSpeech? = null
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    private var isEnabled = true
    
    init {
        initializeTts()
    }
    
    private fun initializeTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.forLanguageTag("vi-VN"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to default locale if Vietnamese not available
                    textToSpeech?.setLanguage(Locale.getDefault())
                }
                _isInitialized.value = true
            } else {
                _isInitialized.value = false
            }
        }
        
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // TTS started
            }
            
            override fun onDone(utteranceId: String?) {
                // TTS completed
            }
            
            override fun onError(utteranceId: String?) {
                // TTS error
            }
        })
    }
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    fun speak(text: String) {
        if (!isEnabled || !_isInitialized.value) return
        
        textToSpeech?.let { tts ->
            // Stop any ongoing speech
            tts.stop()
            
            // Convert number to Vietnamese text
            val vietnameseText = convertNumberToVietnamese(text)
            
            // Speak with unique utterance ID
            tts.speak(
                vietnameseText,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "weight_entry_${System.currentTimeMillis()}"
            )
        }
    }
    
    private fun convertNumberToVietnamese(numberText: String): String {
        return try {
            val number = numberText.toDoubleOrNull() ?: return numberText
            val formatted = String.format(Locale.US, "%.1f", number)
            
            // Simple conversion for common numbers
            // For full implementation, you might want to use a library
            when {
                formatted.endsWith(".0") -> {
                    val wholePart = formatted.substringBefore(".")
                    "$wholePart ký"
                }
                else -> {
                    val parts = formatted.split(".")
                    val whole = parts[0]
                    val decimal = parts.getOrNull(1)?.take(1) ?: "0"
                    "$whole phẩy $decimal ký"
                }
            }
        } catch (e: Exception) {
            numberText
        }
    }
    
    fun stop() {
        textToSpeech?.stop()
    }
    
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        _isInitialized.value = false
    }
}


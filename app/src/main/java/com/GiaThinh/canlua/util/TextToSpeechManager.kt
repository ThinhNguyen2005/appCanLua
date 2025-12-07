package com.GiaThinh.canlua.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextToSpeechManager @Inject constructor(
    private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isEnabled = false
    private var isInitialized = false
    
    fun initialize(onInit: (Boolean) -> Unit = {}) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("vi", "VN"))
                isInitialized = result != TextToSpeech.LANG_MISSING_DATA && 
                               result != TextToSpeech.LANG_NOT_SUPPORTED
                onInit(isInitialized)
            } else {
                isInitialized = false
                onInit(false)
            }
        }
    }
    
    fun speak(text: String, utteranceId: String? = null) {
        if (isEnabled && isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }
    
    fun speakNumber(number: Double) {
        val text = formatNumberForSpeech(number)
        speak(text)
    }
    
    private fun formatNumberForSpeech(number: Double): String {
        // Format số để đọc bằng tiếng Việt
        val formatted = String.format(Locale.US, "%.1f", number)
        return convertToVietnameseNumber(formatted)
    }
    
    private fun convertToVietnameseNumber(numberText: String): String {
        return try {
            val number = numberText.toDoubleOrNull() ?: return numberText
            val parts = numberText.split(".")
            val wholePart = parts[0].toIntOrNull() ?: return numberText
            val decimalPart = parts.getOrNull(1)?.take(1)?.toIntOrNull() ?: 0
            
            val wholeText = numberToVietnamese(wholePart)
            val decimalText = if (decimalPart > 0) {
                " phẩy ${numberToVietnamese(decimalPart)}"
            } else {
                ""
            }
            
            "$wholeText$decimalText ký"
        } catch (e: Exception) {
            numberText
        }
    }
    
    private fun numberToVietnamese(number: Int): String {
        if (number == 0) return "không"
        if (number < 10) {
            return when (number) {
                1 -> "một"
                2 -> "hai"
                3 -> "ba"
                4 -> "bốn"
                5 -> "năm"
                6 -> "sáu"
                7 -> "bảy"
                8 -> "tám"
                9 -> "chín"
                else -> number.toString()
            }
        }
        if (number < 100) {
            val tens = number / 10
            val ones = number % 10
            val tensText = when (tens) {
                1 -> "mười"
                2 -> "hai mươi"
                3 -> "ba mươi"
                4 -> "bốn mươi"
                5 -> "năm mươi"
                6 -> "sáu mươi"
                7 -> "bảy mươi"
                8 -> "tám mươi"
                9 -> "chín mươi"
                else -> ""
            }
            if (ones == 0) return tensText
            val onesText = if (ones == 1 && tens == 1) "một" else numberToVietnamese(ones)
            return "$tensText $onesText"
        }
        return number.toString()
    }
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    fun isEnabled(): Boolean = isEnabled
    
    fun stop() {
        tts?.stop()
    }
    
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}


package com.giathinh.canlua.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper cho [SpeechRecognizer] hỗ trợ STT tiếng Việt.
 *
 * Lý do dùng [SpeechRecognizer] thay vì [RecognizerIntent.ACTION_RECOGNIZE_SPEECH]:
 *  - Không bật dialog hệ thống → UX trong app mượt hơn
 *  - Có partial result → user thấy text live trong khi nói
 *  - Tự xử lý được state (idle / listening / processing)
 */
@Singleton
class SpeechRecognizerHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    enum class State { IDLE, LISTENING, PROCESSING, ERROR }

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var recognizer: SpeechRecognizer? = null
    private var onFinalText: ((String) -> Unit)? = null

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(onResult: (String) -> Unit) {
        if (!isAvailable) {
            _errorMessage.value = "Thiết bị không hỗ trợ nhận dạng giọng nói"
            _state.value = State.ERROR
            return
        }
        if (_state.value == State.LISTENING) return

        onFinalText = onResult
        _partialText.value = ""
        _errorMessage.value = null

        try {
            // SpeechRecognizer phải tạo và destroy trên main thread
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(listener)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            }
            recognizer?.startListening(intent)
            _state.value = State.LISTENING
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = "Không thể khởi chạy nhận dạng giọng nói. Bà con vui lòng cài đặt/bật Google Speech Services hoặc gõ phím để chat."
            _state.value = State.ERROR
            recognizer = null
        }
    }

    fun stop() {
        try {
            recognizer?.stopListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancel() {
        try {
            recognizer?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _state.value = State.IDLE
        _partialText.value = ""
    }

    fun release() {
        try {
            recognizer?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recognizer = null
        _state.value = State.IDLE
    }

    fun clearError() {
        _errorMessage.value = null
        if (_state.value == State.ERROR) _state.value = State.IDLE
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            _state.value = State.PROCESSING
        }

        override fun onError(error: Int) {
            val msg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Lỗi audio"
                SpeechRecognizer.ERROR_CLIENT -> "Lỗi client"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Cần cấp quyền micro"
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Lỗi mạng — kiểm tra Internet"
                SpeechRecognizer.ERROR_NO_MATCH -> "Không nghe rõ — thử lại"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Đang bận, thử lại"
                SpeechRecognizer.ERROR_SERVER -> "Lỗi server"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Hết thời gian chờ giọng nói"
                else -> "Lỗi không xác định ($error)"
            }
            _errorMessage.value = msg
            _state.value = State.ERROR
            _partialText.value = ""
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotBlank()) onFinalText?.invoke(text)
            _state.value = State.IDLE
            _partialText.value = ""
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotBlank()) _partialText.value = text
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}

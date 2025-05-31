package com.example.pla.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class SpeechRecognizerManager(private val context: Context) {
    private val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val permissionManager = PermissionManager(context)

    init {
        setupSpeechRecognizer()
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
            }

            override fun onError(error: Int) {
                _isListening.value = false
                // Можно добавить обработку конкретных ошибок здесь
                when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        _recognizedText.value = "Нет разрешения на использование микрофона"
                    }
                    SpeechRecognizer.ERROR_NO_MATCH -> {
                        _recognizedText.value = "Речь не распознана"
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                        _recognizedText.value = "Распознавание уже идет"
                    }
                    SpeechRecognizer.ERROR_NETWORK -> {
                        _recognizedText.value = "Ошибка сети"
                    }
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                        _recognizedText.value = "Превышено время ожидания сети"
                    }
                    SpeechRecognizer.ERROR_CLIENT -> {
                        _recognizedText.value = "Ошибка клиента распознавания"
                    }
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        _recognizedText.value = "Не удалось распознать речь"
                    }
                    SpeechRecognizer.ERROR_SERVER -> {
                        _recognizedText.value = "Ошибка сервера распознавания"
                    }
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> {
                        _recognizedText.value = "Язык не поддерживается"
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _recognizedText.value = matches[0]
                }
                _isListening.value = false
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _recognizedText.value = matches[0]
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening() {
        if (!permissionManager.hasRecordAudioPermission()) {
            _recognizedText.value = "Нет разрешения на использование микрофона"
            return
        }
        
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _recognizedText.value = "Распознавание речи недоступно на устройстве"
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer.stopListening()
        _isListening.value = false
    }

    fun destroy() {
        speechRecognizer.destroy()
    }
} 
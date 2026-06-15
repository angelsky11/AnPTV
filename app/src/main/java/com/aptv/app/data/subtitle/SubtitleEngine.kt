package com.aptv.app.data.subtitle

import android.content.Context
import com.aptv.app.data.model.subtitle.SubtitleSegment
import com.aptv.app.data.model.subtitle.SubtitleEngineType
import com.aptv.app.data.model.subtitle.SubtitleSettings
import com.aptv.app.data.translation.TranslatorEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 字幕引擎统一入口：
 * 负责协调音频捕获 + 语音识别 + 翻译
 * 高级版专属功能
 */
class SubtitleEngine(
    private val context: Context,
    private val settings: SubtitleSettings,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private var generator: SubtitleGenerator? = null
    private var translatorEngine: TranslatorEngine? = null

    private val _currentSegment = MutableStateFlow<SubtitleSegment?>(null)
    val currentSegment: StateFlow<SubtitleSegment?> = _currentSegment.asStateFlow()

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    /**
     * 启动字幕识别 + 翻译
     */
    fun start() {
        // 检查引擎类型并初始化
        generator = when (settings.engineType) {
            SubtitleEngineType.VOSK -> VoskGenerator(context)
            SubtitleEngineType.WHISPER_CPP -> WhisperCppGenerator(context)
            SubtitleEngineType.GOOGLE_SPEECH -> null // 在线识别需要接入 Google Cloud Speech API
            SubtitleEngineType.WHISPER_API -> null
            SubtitleEngineType.NONE -> null
        }

        generator?.initialize("auto")

        // 翻译引擎
        translatorEngine = TranslatorEngine(settings)

        // 订阅识别结果流
        coroutineScope.launch {
            generator?.observeSegments()?.collect { segment ->
                // 异步翻译
                val translatedText = if (settings.translationModel != null &&
                    settings.translationModel != com.aptv.app.data.model.subtitle.TranslationModel.NONE
                ) {
                    translatorEngine?.translateText(
                        segment.text,
                        settings.sourceLanguage,
                        settings.targetLanguage
                    )
                } else null

                _currentSegment.value = segment.copy(translatedText = translatedText)
            }
        }

        _isEnabled.value = true
    }

    /**
     * 停止并释放资源
     */
    fun stop() {
        generator?.stop()
        generator = null
        translatorEngine = null
        _currentSegment.value = null
        _isEnabled.value = false
    }

    /**
     * 手动输入 PCM 数据（用于外部音频源）
     */
    fun feedAudio(pcmData: ByteArray, sampleRate: Int = 16000) {
        generator?.feedAudio(pcmData, sampleRate)
    }

    /**
     * 更新设置（会重启引擎）
     */
    fun updateSettings(newSettings: SubtitleSettings) {
        if (_isEnabled.value) {
            stop()
            start()
        }
    }
}

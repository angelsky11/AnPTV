package com.aptv.app.data.subtitle

import android.content.Context
import com.aptv.app.data.model.subtitle.SubtitleSegment
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.vosk.Model
import org.vosk.Recognizer

/**
 * Vosk 离线语音识别（轻量离线方案，推荐 Android 端首选）
 * 需要依赖：implementation 'ai.vosk.android:vosk-android:0.3.45'
 *
 * 注意：模型文件需要放在 assets/models/{lang}/ 目录下
 * 中文模型：vosk-model-small-cn-0.3
 * 英文模型：vosk-model-small-en-us-0.15
 */
class VoskGenerator(
    private val context: Context,
    private val modelName: String = "model-small-cn"
) : SubtitleGenerator {

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var segmentId = 0L
    private var startTime = 0L

    override fun initialize(languageCode: String) {
        // 初始化 Vosk 模型
        // 实际使用时需要从 assets 解压模型
        // 示例：
        // val sync = org.vosk.android.StorageService.sync(context, modelName, object : org.vosk.android.StorageService.Callback {
        //     override fun onSuccess(m: Model?) {
        //         model = m
        //     }
        //     override fun onError(e: Exception?) {}
        // })
        // model = Model(modelPath)
    }

    override fun feedAudio(pcmData: ByteArray, sampleRate: Int) {
        if (recognizer == null) {
            val m = model ?: return
            recognizer = Recognizer(m, sampleRate.toFloat())
            startTime = System.currentTimeMillis()
        }

        recognizer?.acceptWaveform(pcmData, pcmData.size)
    }

    override fun observeSegments(): Flow<SubtitleSegment> = callbackFlow {
        val text = recognizer?.result ?: ""
        val partial = recognizer?.partialResult ?: ""
        val finalText = if (text.isNotEmpty()) text else partial

        trySend(
            SubtitleSegment(
                id = segmentId++,
                text = finalText,
                startTimeMs = startTime,
                endTimeMs = System.currentTimeMillis(),
                language = "zh"
            )
        )

        awaitClose { /* 保持流打开 */ }
    }

    override fun stop() {
        recognizer?.close()
        model?.close()
        recognizer = null
        model = null
    }

    override fun isAvailable(): Boolean = model != null
}

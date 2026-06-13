package com.aptv.app.data.subtitle

import android.content.Context
import com.aptv.app.data.model.subtitle.SubtitleSegment
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Whisper.cpp 本地语音识别（更强大的离线方案，需要编译 JNI）
 *
 * 模型文件建议：ggml-small.bin (约 500MB，中英文效果较好)
 * 轻量版本：ggml-base.bin (约 150MB)
 *
 * 使用方式：
 * 1. 在 build.gradle.kts 中加入 JNI 编译配置
 * 2. 编译 whisper.cpp 的 Android 版本
 * 3. 在 src/main/jniLibs/{abi}/ 中放置 .so 文件
 *
 * 本文件提供 JNI 接口示例。实际实现需要编译 whisper.cpp 的 JNI 部分。
 */
class WhisperCppGenerator(
    private val context: Context,
    private val modelAssetPath: String = "models/ggml-base.bin"
) : SubtitleGenerator {

    private var whisperContext: Long = 0
    private var currentLanguage = "auto"
    private var segmentIdCounter = 0L

    // JNI 接口声明（实际实现时需要编译 whisper.cpp 的 Android JNI 库）
    // companion object {
    //     init {
    //         System.loadLibrary("whisper")
    //     }
    // }
    //
    // external fun initContext(modelPath: String): Long
    // external fun freeContext(ctx: Long)
    // external fun transcribe(ctx: Long, pcm: FloatArray, language: String): String

    override fun initialize(languageCode: String) {
        currentLanguage = languageCode
        // 从 assets 复制到应用私有目录（防止只读）
        val modelFile = java.io.File(context.filesDir, "ggml-base.bin")
        if (!modelFile.exists()) {
            try {
                context.assets.open(modelAssetPath).use { input ->
                    modelFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                // 模型文件缺失
            }
        }
        // whisperContext = initContext(modelFile.absolutePath)
    }

    override fun feedAudio(pcmData: ByteArray, sampleRate: Int) {
        // ByteArray -> FloatArray (16-bit PCM -> normalized float)
        val floatData = FloatArray(pcmData.size / 2) { index ->
            val sample = (pcmData[index * 2].toInt() and 0xFF) or
                    (pcmData[index * 2 + 1].toInt() shl 8)
            (sample.toFloat() / 32768.0f).coerceIn(-1.0f, 1.0f)
        }
        // val result = transcribe(whisperContext, floatData, currentLanguage)
        // 结果保存在内部缓冲
    }

    override fun observeSegments(): Flow<SubtitleSegment> = callbackFlow {
        trySend(
            SubtitleSegment(
                id = segmentIdCounter++,
                text = "Whisper 识别结果占位",
                startTimeMs = System.currentTimeMillis(),
                endTimeMs = System.currentTimeMillis()
            )
        )
        awaitClose { /* 保持流打开 */ }
    }

    override fun stop() {
        // if (whisperContext != 0L) {
        //     freeContext(whisperContext)
        //     whisperContext = 0L
        // }
    }

    override fun isAvailable(): Boolean = whisperContext != 0L
}

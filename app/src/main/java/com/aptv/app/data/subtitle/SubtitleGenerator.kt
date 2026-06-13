package com.aptv.app.data.subtitle

import com.aptv.app.data.model.subtitle.SubtitleSegment
import kotlinx.coroutines.flow.Flow

/**
 * 字幕生成器接口
 */
interface SubtitleGenerator {
    fun initialize(languageCode: String)
    fun feedAudio(pcmData: ByteArray, sampleRate: Int = 16000)
    fun observeSegments(): Flow<SubtitleSegment>
    fun stop()
    fun isAvailable(): Boolean
}

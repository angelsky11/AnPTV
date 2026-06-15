package com.aptv.app.data.translation

/**
 * 翻译引擎统一接口
 */
interface SubtitleTranslator {
    suspend fun translate(
        text: String,
        sourceLang: String = "auto",
        targetLang: String = "zh"
    ): String?

    fun requiresApiKey(): Boolean = false
    fun isAvailable(): Boolean = true
    fun displayName(): String
}

package com.aptv.app.data.translation

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

/**
 * Google ML Kit 离线翻译（完全无需联网，无需 API Key）
 *
 * 首次使用前需要下载对应语言的翻译模型。
 * 支持 50+ 语言互译，首次翻译某一方向时需要下载约 10-30MB 的模型。
 *
 * 依赖：implementation 'com.google.mlkit:translate:17.0.2'
 */
class MLKitTranslator : SubtitleTranslator {

    private var currentTranslator: com.google.mlkit.nl.translate.Translator? = null
    private var currentSource: String? = null
    private var currentTarget: String? = null

    override fun displayName(): String = "ML Kit 离线翻译"

    override fun requiresApiKey(): Boolean = false

    override fun isAvailable(): Boolean = currentTranslator != null

    private fun getOrCreateTranslator(sourceLang: String, targetLang: String): com.google.mlkit.nl.translate.Translator {
        if (currentSource == sourceLang && currentTarget == targetLang && currentTranslator != null) {
            return currentTranslator!!
        }

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(mapLanguageCode(sourceLang))
            .setTargetLanguage(mapLanguageCode(targetLang))
            .build()

        currentTranslator?.close()
        currentTranslator = Translation.getClient(options)
        currentSource = sourceLang
        currentTarget = targetLang
        return currentTranslator!!
    }

    /**
     * 下载翻译模型（异步）
     */
    suspend fun ensureModelDownloaded(sourceLang: String, targetLang: String): Boolean {
        return try {
            val translator = getOrCreateTranslator(sourceLang, targetLang)
            translator.downloadModelIfNeeded().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String? {
        return try {
            val translator = getOrCreateTranslator(sourceLang, targetLang)
            translator.translate(text).await()
        } catch (e: Exception) {
            null
        }
    }

    private fun mapLanguageCode(code: String): String {
        return when (code.lowercase()) {
            "zh", "cmn", "zh-cn", "chinese" -> TranslateLanguage.CHINESE
            "en", "english" -> TranslateLanguage.ENGLISH
            "ja", "jp", "japanese" -> TranslateLanguage.JAPANESE
            "ko", "korean" -> TranslateLanguage.KOREAN
            "fr", "french" -> TranslateLanguage.FRENCH
            "de", "german" -> TranslateLanguage.GERMAN
            "es", "spanish" -> TranslateLanguage.SPANISH
            "ru", "russian" -> TranslateLanguage.RUSSIAN
            "ar", "arabic" -> TranslateLanguage.ARABIC
            "it", "italian" -> TranslateLanguage.ITALIAN
            "pt", "portuguese" -> TranslateLanguage.PORTUGUESE
            "auto" -> TranslateLanguage.ENGLISH // 先识别，实际使用语言检测模块
            else -> TranslateLanguage.ENGLISH
        }
    }
}

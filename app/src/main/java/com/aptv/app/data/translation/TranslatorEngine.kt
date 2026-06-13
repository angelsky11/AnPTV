package com.aptv.app.data.translation

import com.aptv.app.data.model.subtitle.SubtitleSettings
import com.aptv.app.data.model.subtitle.TranslationModel

/**
 * 翻译引擎统一选择器（根据设置选择具体的翻译模型）
 */
class TranslatorEngine(
    private val settings: SubtitleSettings
) {

    private var cachedTranslator: SubtitleTranslator? = null

    /**
     * 获取当前配置的翻译引擎（可能为 null，当设置为 NONE 或未配置）
     */
    fun getTranslator(): SubtitleTranslator? {
        if (cachedTranslator != null) return cachedTranslator

        cachedTranslator = when (settings.translationModel) {
            TranslationModel.NONE -> null

            TranslationModel.ML_KIT_OFFLINE -> MLKitTranslator()

            TranslationModel.OPENAI_GPT -> {
                if (settings.apiKeyOpenAI.isNotBlank()) {
                    OpenAITranslator(settings.apiKeyOpenAI)
                } else null
            }

            TranslationModel.GOOGLE_CLOUD -> {
                // Google Cloud Translation 简化版（使用 API Key）
                if (settings.apiKeyGoogleCloud.isNotBlank()) {
                    GoogleCloudTranslator(settings.apiKeyGoogleCloud)
                } else null
            }

            TranslationModel.DEEPL -> {
                if (settings.apiKeyGoogleCloud.isNotBlank()) {
                    DeepLTranslator(settings.apiKeyGoogleCloud)
                } else null
            }

            TranslationModel.BAIDU -> {
                if (settings.apiKeyBaiduAppId.isNotBlank() && settings.apiKeyBaiduSecret.isNotBlank()) {
                    BaiduTranslator(settings.apiKeyBaiduAppId, settings.apiKeyBaiduSecret)
                } else null
            }

            TranslationModel.TENCENT -> {
                if (settings.apiKeyTencentSecretId.isNotBlank() && settings.apiKeyTencentSecretKey.isNotBlank()) {
                    TencentTranslator(settings.apiKeyTencentSecretId, settings.apiKeyTencentSecretKey)
                } else null
            }
        }

        return cachedTranslator
    }

    /**
     * 翻译一段文本（方便函数）
     */
    suspend fun translateText(
        text: String,
        sourceLang: String = settings.sourceLanguage,
        targetLang: String = settings.targetLanguage
    ): String? {
        val translator = getTranslator() ?: return null
        return translator.translate(text, sourceLang, targetLang)
    }

    fun resetCache() {
        cachedTranslator = null
    }
}

/**
 * Google Cloud Translation API 简化实现
 */
class GoogleCloudTranslator(
    private val apiKey: String
) : SubtitleTranslator {

    override fun displayName(): String = "Google Cloud"

    override fun requiresApiKey(): Boolean = true

    override fun isAvailable(): Boolean = apiKey.isNotBlank()

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String? {
        return try {
            val url = java.net.URL(
                "https://translation.googleapis.com/language/translate/v2" +
                        "?q=${java.net.URLEncoder.encode(text, "UTF-8")}" +
                        "&source=$sourceLang&target=$targetLang&key=$apiKey"
            )
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 30000

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = org.json.JSONObject(response)
            val data = json.getJSONObject("data")
            val translations = data.getJSONArray("translations")
            if (translations.length() > 0) {
                translations.getJSONObject(0).getString("translatedText")
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * DeepL Pro 翻译 API
 */
class DeepLTranslator(
    private val apiKey: String,
    private val useFree: Boolean = true
) : SubtitleTranslator {

    override fun displayName(): String = "DeepL"

    override fun requiresApiKey(): Boolean = true

    override fun isAvailable(): Boolean = apiKey.isNotBlank()

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String? {
        return try {
            val baseUrl = if (useFree) "api-free.deepl.com" else "api.deepl.com"
            val body = "text=${java.net.URLEncoder.encode(text, "UTF-8")}" +
                    "&target_lang=${targetLang.uppercase()}" +
                    if (sourceLang != "auto") "&source_lang=${sourceLang.uppercase()}" else ""

            val url = java.net.URL("https://$baseUrl/v2/translate")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "DeepL-Auth-Key $apiKey")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 30000
            }

            connection.outputStream.use { it.write(body.toByteArray()) }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = org.json.JSONObject(response)
            val translations = json.getJSONArray("translations")
            if (translations.length() > 0) {
                translations.getJSONObject(0).getString("text")
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

package com.aptv.app.data.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * OpenAI / Gemini 在线翻译（通过 HTTP API 调用）
 *
 * 模型：gpt-4o-mini（速度快，成本低，适合实时字幕翻译）
 * API Key 申请：https://platform.openai.com/api-keys
 */
class OpenAITranslator(
    private val apiKey: String,
    private val model: String = "gpt-4o-mini"
) : SubtitleTranslator {

    override fun displayName(): String = "OpenAI GPT"

    override fun requiresApiKey(): Boolean = true

    override fun isAvailable(): Boolean = apiKey.isNotBlank()

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null

        return@withContext try {
            val sourceName = languageName(sourceLang)
            val targetName = languageName(targetLang)

            val systemPrompt = """
                你是一个专业的字幕翻译助手。请将以下文本从$sourceName翻译成$targetName。
                要求：1. 口语化、自然流畅 2. 不要添加解释说明 3. 只返回翻译结果。
            """.trimIndent()

            val userPrompt = text

            val requestBody = """
                {
                    "model": "$model",
                    "messages": [
                        {"role": "system", "content": "$systemPrompt"},
                        {"role": "user", "content": "$userPrompt"}
                    ],
                    "max_tokens": 200,
                    "temperature": 0.3
                }
            """.trimIndent()

            val url = URL("https://api.openai.com/v1/chat/completions")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 30000
            }

            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)

            json
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        } catch (e: Exception) {
            null
        }
    }

    private fun languageName(code: String): String {
        return when (code.lowercase()) {
            "zh" -> "中文"
            "en" -> "英文"
            "ja" -> "日文"
            "ko" -> "韩文"
            "fr" -> "法文"
            "de" -> "德文"
            "es" -> "西班牙文"
            "ru" -> "俄文"
            else -> code
        }
    }
}

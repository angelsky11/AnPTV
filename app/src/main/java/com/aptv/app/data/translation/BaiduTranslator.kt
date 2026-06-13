package com.aptv.app.data.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

/**
 * 百度翻译（百度翻译开放平台）
 *
 * 免费额度：标准版每月 200 万字符
 * 申请地址：https://fanyi-api.baidu.com/
 *
 * 认证方式：appid + salt + MD5(appid + q + salt + secret)
 * API: https://fanyi-api.baidu.com/api/trans/vip/translate
 */
class BaiduTranslator(
    private val appId: String,
    private val secretKey: String
) : SubtitleTranslator {

    override fun displayName(): String = "百度翻译"

    override fun requiresApiKey(): Boolean = true

    override fun isAvailable(): Boolean = appId.isNotBlank() && secretKey.isNotBlank()

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String? = withContext(Dispatchers.IO) {
        if (appId.isBlank() || secretKey.isBlank()) return@withContext null

        return@withContext try {
            val salt = System.currentTimeMillis().toString()
            val sign = md5(appId + text + salt + secretKey)

            val from = mapLanguageCode(sourceLang)
            val to = mapLanguageCode(targetLang)

            // POST form data
            val formData = StringBuilder().apply {
                append("q=").append(URLEncoder.encode(text, "UTF-8"))
                append("&from=").append(from)
                append("&to=").append(to)
                append("&appid=").append(appId)
                append("&salt=").append(salt)
                append("&sign=").append(sign)
            }.toString()

            val url = URL("https://fanyi-api.baidu.com/api/trans/vip/translate")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 30000
            }

            connection.outputStream.use { it.write(formData.toByteArray()) }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)

            if (json.has("error_code")) {
                // 失败
                null
            } else {
                val results = json.getJSONArray("trans_result")
                if (results.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until results.length()) {
                        sb.append(results.getJSONObject(i).getString("dst"))
                        if (i < results.length() - 1) sb.append(" ")
                    }
                    sb.toString()
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun mapLanguageCode(code: String): String {
        return when (code.lowercase()) {
            "zh", "cmn", "zh-cn", "chinese" -> "zh"
            "en", "english" -> "en"
            "ja", "jp", "japanese" -> "jp"
            "ko", "kor", "korean" -> "kor"
            "fr", "french" -> "fra"
            "de", "german" -> "de"
            "es", "spanish" -> "spa"
            "ru", "russian" -> "ru"
            "it", "italian" -> "it"
            "pt", "portuguese" -> "pt"
            "ar", "arabic" -> "ara"
            "auto" -> "auto"
            else -> "auto"
        }
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray(charset("UTF-8")))
        val hexString = StringBuilder()
        for (b in digest) {
            val hex = Integer.toHexString(0xFF and b.toInt())
            if (hex.length == 1) hexString.append('0')
            hexString.append(hex)
        }
        return hexString.toString()
    }
}

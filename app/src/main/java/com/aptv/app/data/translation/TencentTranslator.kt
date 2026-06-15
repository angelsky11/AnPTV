package com.aptv.app.data.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 腾讯云翻译（Tencent Cloud Translation - TextTranslate）
 *
 * 认证方式：SecretId + SecretKey + HMAC-SHA256 签名
 * 申请地址：https://console.cloud.tencent.com/cam/capi
 * API: https://tmt.tencentcloudapi.com/
 */
class TencentTranslator(
    private val secretId: String,
    private val secretKey: String,
    private val projectId: Long = 0
) : SubtitleTranslator {

    private val service = "tmt"
    private val host = "tmt.tencentcloudapi.com"
    private val version = "2018-03-02"
    private val action = "TextTranslate"
    private val algorithm = "TC3-HMAC-SHA256"

    override fun displayName(): String = "腾讯翻译"

    override fun requiresApiKey(): Boolean = true

    override fun isAvailable(): Boolean = secretId.isNotBlank() && secretKey.isNotBlank()

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String? = withContext(Dispatchers.IO) {
        if (secretId.isBlank() || secretKey.isBlank()) return@withContext null

        return@withContext try {
            val from = mapLanguageCode(sourceLang)
            val to = mapLanguageCode(targetLang)

            val timestamp = (System.currentTimeMillis() / 1000).toString()
            val date = java.text.SimpleDateFormat("yyyy-MM-dd")
                .format(java.util.Date(timestamp.toLong() * 1000))

            // Step 1: HTTP 请求规范
            val httpRequest = """
                POST
                /
                content-type:application/json
                host:$host

                content-type;host
                ${sha256("{\"SourceText\":\"$text\",\"Source\":\"$from\",\"Target\":\"$to\",\"ProjectId\":$projectId}")}
            """.trimIndent()

            // Step 2: 签名字符串
            val credentialScope = "$date/$service/tc3_request"
            val stringToSign = """
                $algorithm
                $timestamp
                $credentialScope
                ${sha256(httpRequest)}
            """.trimIndent()

            // Step 3: 计算签名
            val secretDate = hmac256("TC3$secretKey".toByteArray(StandardCharsets.UTF_8), date)
            val secretService = hmac256(secretDate, service)
            val secretSigning = hmac256(secretService, "tc3_request")
            val signature = byteArrayToHexString(hmac256(secretSigning, stringToSign))

            // Step 4: Authorization 头
            val authorization = "$algorithm Credential=$secretId/$credentialScope, SignedHeaders=content-type;host, Signature=$signature"

            // 发起请求
            val payload = """{"SourceText":"$text","Source":"$from","Target":"$to","ProjectId":$projectId}"""

            val url = URL("https://$host/")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", authorization)
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Host", host)
                setRequestProperty("X-TC-Action", action)
                setRequestProperty("X-TC-Timestamp", timestamp)
                setRequestProperty("X-TC-Version", version)
                setRequestProperty("X-TC-Region", "ap-guangzhou")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 30000
            }

            connection.outputStream.use { it.write(payload.toByteArray(StandardCharsets.UTF_8)) }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)

            if (json.has("Response")) {
                val resp = json.getJSONObject("Response")
                if (resp.has("Error")) {
                    null
                } else {
                    resp.optString("TargetText")
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun mapLanguageCode(code: String): String {
        return when (code.lowercase()) {
            "zh", "cmn", "zh-cn" -> "zh"
            "zh-tw", "zh-hk" -> "zh-TW"
            "en" -> "en"
            "ja" -> "ja"
            "ko" -> "ko"
            "fr" -> "fr"
            "de" -> "de"
            "es" -> "es"
            "ru" -> "ru"
            "it" -> "it"
            "pt" -> "pt"
            "ar" -> "ar"
            else -> "auto"
        }
    }

    private fun sha256(text: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return byteArrayToHexString(md.digest(text.toByteArray(StandardCharsets.UTF_8)))
    }

    private fun hmac256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
    }

    private fun byteArrayToHexString(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val hex = Integer.toHexString(0xFF and b.toInt())
            if (hex.length == 1) sb.append('0')
            sb.append(hex)
        }
        return sb.toString()
    }
}

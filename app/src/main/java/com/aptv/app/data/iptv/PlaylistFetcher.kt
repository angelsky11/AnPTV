package com.aptv.app.data.iptv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 从网络下载 M3U 播放源
 */
class PlaylistFetcher(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {
    suspend fun fetch(url: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Result.failure(Exception("HTTP ${response.code}"))
                    } else {
                        val body = response.body?.string().orEmpty()
                        Result.success(body)
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

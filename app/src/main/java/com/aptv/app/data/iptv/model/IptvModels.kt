package com.aptv.app.data.iptv.model

/**
 * M3U 播放源（一组频道的来源）
 */
data class PlaylistSource(
    val id: Long = 0,
    val name: String,
    val url: String,
    val addedAt: Long = System.currentTimeMillis(),
    val channelCount: Int = 0,
    val isActive: Boolean = true
)

/**
 * 单个频道（M3U 中的一条 EXTINF）
 */
data class Channel(
    val id: Long = 0,
    val sourceId: Long,
    val name: String,
    val logo: String? = null,
    val group: String = "未分类",
    val url: String,
    val tvgId: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val lastWatchedAt: Long = 0
)

/**
 * EPG 节目单（XMLTV 格式）
 */
data class EpgProgram(
    val id: Long = 0,
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startTime: Long,
    val endTime: Long,
    val category: String? = null
)

/**
 * 频道分组
 */
data class ChannelGroup(
    val name: String,
    val channels: List<Channel>
)

package com.aptv.app.data.iptv

import com.aptv.app.data.iptv.model.Channel

/**
 * M3U / M3U8 播放列表解析器
 */
class M3uParser {

    data class ParsedResult(
        val channels: List<Channel>,
        val name: String
    )

    fun parse(content: String, sourceId: Long, playlistName: String = "默认播放源"): ParsedResult {
        val channels = mutableListOf<Channel>()
        val lines = content.lineSequence().iterator()

        var currentExtInf: ExtInfLine? = null
        var index = 0L

        while (lines.hasNext()) {
            val rawLine = lines.next().trim()
            if (rawLine.isEmpty()) continue

            if (rawLine.startsWith("#EXTINF:")) {
                currentExtInf = parseExtInf(rawLine)
            } else if (!rawLine.startsWith("#") && rawLine.isNotBlank()) {
                val channelName = currentExtInf?.name ?: rawLine.substringAfterLast("/").substringBefore(".")
                val channel = Channel(
                    id = index++,
                    sourceId = sourceId,
                    name = channelName,
                    logo = currentExtInf?.tvgLogo,
                    group = currentExtInf?.groupTitle ?: "未分类",
                    url = rawLine,
                    tvgId = currentExtInf?.tvgId
                )
                channels.add(channel)
                currentExtInf = null
            }
        }

        return ParsedResult(
            channels = channels,
            name = playlistName
        )
    }

    private data class ExtInfLine(
        val name: String,
        val tvgLogo: String?,
        val groupTitle: String?,
        val tvgId: String?
    )

    private fun parseExtInf(line: String): ExtInfLine? {
        val namePart = line.substringAfter("#EXTINF:")
        val durationAndAttrs = namePart.substringBefore(",")
        val name = namePart.substringAfter(",").ifBlank { "未命名频道" }

        val tvgLogo = extractAttribute(
            durationAndAttrs, "tvg-logo")
        val groupTitle = extractAttribute(
            durationAndAttrs, "group-title")
        val tvgId = extractAttribute(
            durationAndAttrs, "tvg-id")

        return ExtInfLine(
            name = name.trim(),
            tvgLogo = tvgLogo?.trim('"', '\''),
            groupTitle = groupTitle?.trim('"', '\''),
        tvgId = tvgId?.trim('"', '\''))
    }

    private fun extractAttribute(line: String, name: String): String? {
        val pattern = """$name="([^"]*)"""".toRegex()
        val result = pattern.find(line) ?: return null
        return result.groupValues.getOrNull(1)
    }
}

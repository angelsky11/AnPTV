package com.aptv.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aptv.app.data.subtitle.SubtitleEngine

/**
 * 字幕覆盖层 UI（Compose）
 * 在视频播放器底部显示字幕，支持原文 + 译文双语显示。
 */
@Composable
fun SubtitleOverlay(
    subtitleEngine: SubtitleEngine,
    fontSize: Int = 20,
    backgroundOpacity: Float = 0.7f,
    showOriginal: Boolean = true,
    modifier: Modifier = Modifier
) {
    val currentSegment by subtitleEngine.currentSegment.collectAsState()
    val isEnabled by subtitleEngine.isEnabled.collectAsState()

    if (!isEnabled || currentSegment == null) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(
                    Color.Black.copy(alpha = backgroundOpacity)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 原文（可选）
            if (showOriginal && !currentSegment?.text.isNullOrEmpty()) {
                Text(
                    text = currentSegment?.text ?: "",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = (fontSize - 2).sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // 译文（如果有），否则显示原文
            val displayText = if (currentSegment?.translatedText?.isNotBlank() == true) {
                currentSegment!!.translatedText
            } else {
                currentSegment?.text
            }

            if (!displayText.isNullOrEmpty()) {
                Text(
                    text = displayText,
                    color = if (currentSegment?.translatedText?.isNotBlank() == true) Color(0xFFFFD700) else Color.White,
                    fontSize = fontSize.sp,
                    fontWeight = if (currentSegment?.translatedText?.isNotBlank() == true) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

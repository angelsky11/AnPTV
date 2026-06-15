package com.aptv.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aptv.app.data.iptv.IptvRepository
import com.aptv.app.data.iptv.model.Channel
import com.aptv.app.data.purchase.PurchaseManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    iptvRepository: IptvRepository,
    purchaseManager: PurchaseManager,
    onAddSource: () -> Unit,
    onGoPremium: () -> Unit,
    onPlayChannel: (Channel) -> Unit
) {
    val channels by iptvRepository.channels.collectAsState()
    val sources by iptvRepository.sources.collectAsState()
    val favorites by iptvRepository.favorites.collectAsState()
    val purchaseState by purchaseManager.purchaseState.collectAsState()
    val isLoading by iptvRepository.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("APTV", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (purchaseState.isPremium) "终身高级版" else "免费版",
                            fontSize = 12.sp,
                            color = if (purchaseState.isPremium) Color(0xFFe94560) else Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onAddSource() }) {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                    IconButton(onClick = {
                        iptvRepository.refreshCurrentSource()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (channels.isEmpty()) {
                EmptyHomeState(onAddSource, onGoPremium, purchaseState.isPremium)
            } else {
                // 统计卡片
                StatsRow(
                    channelCount = channels.size,
                    sourceCount = sources.size,
                    favoriteCount = favorites.size
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 推荐频道
                Text(
                    "推荐频道",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(channels.take(20)) { channel ->
                        ChannelCard(
                            channel = channel,
                            isFavorite = favorites.contains(channel.id),
                            onClick = { onPlayChannel(channel) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 收藏频道
                if (favorites.isNotEmpty()) {
                    Text(
                        "我的收藏",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(channels.filter { favorites.contains(it.id) }) { channel ->
                            ChannelCard(
                                channel = channel,
                                isFavorite = true,
                                onClick = { onPlayChannel(channel) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 高级版入口
                if (!purchaseState.isPremium) {
                    PremiumCard(onGoPremium)
                }
            }
        }
    }
}

@Composable
private fun EmptyHomeState(
    onAddSource: () -> Unit,
    onGoPremium: () -> Unit,
    isPremium: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        Text(
            "欢迎使用 APTV",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "添加一个 M3U 播放源开始看电视",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(32.dp))

        androidx.compose.material3.Button(
            onClick = onAddSource,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加播放源", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isPremium) {
            androidx.compose.material3.OutlinedButton(
                onClick = onGoPremium,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("了解高级版")
            }
        }
    }
}

@Composable
private fun StatsRow(
    channelCount: Int,
    sourceCount: Int,
    favoriteCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard("频道", channelCount, Modifier.weight(1f))
        StatCard("播放源", sourceCount, Modifier.weight(1f))
        StatCard("收藏", favoriteCount, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, count: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$count",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ChannelCard(
    channel: Channel,
    isFavorite: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(120.dp)
            .padding(end = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Tv,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isFavorite) Color(0xFFe94560) else Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = channel.name,
                fontSize = 12.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun PremiumCard(onGoPremium: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onGoPremium),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF16213e)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "APTV 终身高级版",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "无限播放源 · 实时字幕 · 去除广告",
                    color = Color(0xFFCCCCCC),
                    fontSize = 13.sp
                )
            }
            Text(
                "¥45",
                color = Color(0xFFe94560),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

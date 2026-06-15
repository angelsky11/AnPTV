package com.aptv.app.ui.channels

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aptv.app.data.iptv.IptvRepository
import com.aptv.app.data.iptv.model.Channel
import com.aptv.app.data.iptv.model.PlaylistSource
import com.aptv.app.data.purchase.PurchaseManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    iptvRepository: IptvRepository,
    purchaseManager: PurchaseManager,
    onAddSource: () -> Unit,
    onGoPremium: () -> Unit,
    onPlayChannel: (Channel) -> Unit
) {
    val channels by iptvRepository.channels.collectAsState()
    val sources by iptvRepository.sources.collectAsState()
    val currentSourceId by iptvRepository.currentSourceId.collectAsState()
    val favorites by iptvRepository.favorites.collectAsState()
    val isLoading by iptvRepository.isLoading.collectAsState()
    val purchaseState by purchaseManager.purchaseState.collectAsState()

    var selectedSourceId by remember(currentSourceId) {
        mutableStateOf(currentSourceId)
    }

    val filteredChannels = channels.filter {
        selectedSourceId == null || it.sourceId == selectedSourceId
    }

    val groupedChannels = filteredChannels.groupBy { it.group }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("频道", fontWeight = FontWeight.Bold)
                        if (sources.isNotEmpty()) {
                            Text(
                                "${filteredChannels.size} 个频道",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (sources.isEmpty() || purchaseState.isPremium) {
                            onAddSource()
                        } else {
                            onGoPremium()
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 播放源选择器
            if (sources.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sources) { source ->
                        SourceChip(
                            source = source,
                            isSelected = source.id == selectedSourceId,
                            onClick = { selectedSourceId = source.id; iptvRepository.selectSource(source.id) }
                        )
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredChannels.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (sources.isEmpty()) {
                            Text("暂无频道", color = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Button(
                                onClick = {
                                    if (purchaseState.isPremium || sources.isEmpty()) {
                                        onAddSource()
                                    } else {
                                        onGoPremium()
                                    }
                                }
                            ) {
                                Text("添加播放源")
                            }
                        } else {
                            Text("此播放源暂无频道", color = Color.Gray)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    groupedChannels.forEach { (group, channelsInGroup) ->
                        item {
                            Text(
                                text = group,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(channelsInGroup) { channel ->
                            ChannelListItem(
                                channel = channel,
                                isFavorite = favorites.contains(channel.id),
                                onPlay = { onPlayChannel(channel) },
                                onToggleFavorite = { iptvRepository.toggleFavorite(channel.id) }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SourceChip(
    source: PlaylistSource,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = source.name,
                color = if (isSelected) Color.White else Color.Black,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${source.channelCount} 频道",
                color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ChannelListItem(
    channel: Channel,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Tv,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = channel.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = channel.group,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = if (isFavorite) Color(0xFFe94560) else Color.Gray
            )
        }
    }
}

package com.aptv.app.ui.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    iptvRepository: IptvRepository,
    onPlayChannel: (Channel) -> Unit
) {
    val channels by iptvRepository.channels.collectAsState()
    val favorites by iptvRepository.favorites.collectAsState()

    val favoriteChannels = channels.filter { favorites.contains(it.id) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("收藏", fontWeight = FontWeight.Bold)
                        Text(
                            "${favoriteChannels.size} 个频道",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
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
            if (favoriteChannels.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFCCCCCC),
                            modifier = Modifier.width(64.dp)
                        )
                        Spacer(modifier = androidx.compose.ui.Modifier)
                        Text(
                            "还没有收藏的频道",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier)
                        Text(
                            "点击频道右侧的 ☆ 图标即可收藏",
                            color = Color(0xFF999999),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(favoriteChannels) { channel ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlayChannel(channel) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFe94560)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
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
                        }
                    }
                }
            }
        }
    }
}

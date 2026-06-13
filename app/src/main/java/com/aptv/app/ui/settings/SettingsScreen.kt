package com.aptv.app.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.aptv.app.data.purchase.PurchaseManager
import com.aptv.app.data.purchase.model.PremiumFeature
import com.aptv.app.data.purchase.model.PurchaseState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    iptvRepository: IptvRepository,
    purchaseManager: PurchaseManager,
    onGoPremium: () -> Unit
) {
    val sources by iptvRepository.sources.collectAsState()
    val purchaseState by purchaseManager.purchaseState.collectAsState()
    val errorMessage by iptvRepository.errorMessage.collectAsState()
    val isLoading by iptvRepository.isLoading.collectAsState()

    var showAddSourceDialog by remember { mutableStateOf(false) }
    var newSourceName by remember { mutableStateOf("") }
    var newSourceUrl by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("设置", fontWeight = FontWeight.Bold)
                        Text(
                            if (purchaseState.isPremium) "高级版已激活" else "免费版",
                            fontSize = 12.sp,
                            color = if (purchaseState.isPremium) Color(0xFFe94560) else Color.Gray
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
                .padding(16.dp)
        ) {
            // 高级版状态卡片
            if (!purchaseState.isPremium) {
                PremiumPromoCard(onGoPremium)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 播放源管理
            Text(
                "播放源管理",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (sources.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("暂无播放源", color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { showAddSourceDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("添加播放源")
                        }
                    }
                }
            } else {
                sources.forEach { source ->
                    SourceCard(
                        name = source.name,
                        url = source.url,
                        channelCount = source.channelCount,
                        onRefresh = {
                            iptvRepository.selectSource(source.id)
                            iptvRepository.refreshCurrentSource()
                        },
                        onRemove = {
                            iptvRepository.removeSource(source.id)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 添加播放源按钮
            Button(
                onClick = {
                    if (sources.isEmpty() || purchaseState.isPremium) {
                        showAddSourceDialog = true
                    } else {
                        onGoPremium()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (sources.isEmpty() || purchaseState.isPremium)
                        "添加新播放源"
                    else
                        "升级高级版以添加更多播放源"
                )
            }

            // 字幕与翻译（高级功能）
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "字幕与翻译",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            PremiumFeatureCard(
                title = "实时字幕识别",
                description = "通过 AI 语音识别生成字幕，支持 Vosk/Whisper 多种引擎",
                icon = Icons.Default.Subtitles,
                isPremium = purchaseState.isPremium,
                onGoPremium = onGoPremium
            )
            Spacer(modifier = Modifier.height(8.dp))

            PremiumFeatureCard(
                title = "字幕翻译",
                description = "支持 Google 翻译 / 百度翻译 / 腾讯翻译 / DeepL 等多种翻译引擎",
                icon = Icons.Default.Translate,
                isPremium = purchaseState.isPremium,
                onGoPremium = onGoPremium
            )

            // 添加播放源对话框
            if (showAddSourceDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showAddSourceDialog = false
                        newSourceName = ""
                        newSourceUrl = ""
                    },
                    title = { Text("添加播放源") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = newSourceName,
                                onValueChange = { newSourceName = it },
                                label = { Text("名称（可选）") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = newSourceUrl,
                                onValueChange = { newSourceUrl = it },
                                label = { Text("M3U 地址") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "支持 M3U / M3U8 格式的 IPTV 播放源",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (newSourceUrl.isNotBlank()) {
                                    val name = newSourceName.ifBlank {
                                        newSourceUrl.substringAfterLast("/").substringBefore(".")
                                    }
                                    iptvRepository.addPlaylistSource(
                                        newSourceUrl.trim(),
                                        name
                                    )
                                    showAddSourceDialog = false
                                    newSourceName = ""
                                    newSourceUrl = ""
                                }
                            }
                        ) {
                            Text("添加")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showAddSourceDialog = false
                            newSourceName = ""
                            newSourceUrl = ""
                        }) {
                            Text("取消")
                        }
                    }
                )
            }

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(msg, color = Color.Red, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SourceCard(
    name: String,
    url: String,
    channelCount: Int,
    onRefresh: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    url,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
                Text(
                    "$channelCount 个频道",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新")
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = Color(0xFFe94560)
                )
            }
        }
    }
}

@Composable
private fun PremiumPromoCard(onGoPremium: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF16213e)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp)
        ) {
            Text(
                "升级为 APTV 终身高级版",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "无限播放源 · 实时字幕 · 多语言翻译 · 去除广告",
                color = Color(0xFFCCCCCC),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "¥45",
                    color = Color(0xFFe94560),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "终身授权",
                    color = Color.White,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onGoPremium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFe94560)
                    )
                ) {
                    Text("立即升级")
                }
            }
        }
    }
}

@Composable
private fun PremiumFeatureCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPremium: Boolean,
    onGoPremium: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isPremium) Color(0xFF4CAF50) else Color.Gray
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, fontSize = 12.sp, color = Color.Gray)
            }
            if (!isPremium) {
                OutlinedButton(onClick = onGoPremium) {
                    Text("升级", fontSize = 12.sp)
                }
            } else {
                Text("已解锁", fontSize = 12.sp, color = Color(0xFF4CAF50))
            }
        }
    }
}

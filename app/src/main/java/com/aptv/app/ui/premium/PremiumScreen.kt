package com.aptv.app.ui.premium

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.ProductDetails
import com.aptv.app.data.purchase.PurchaseManager
import com.aptv.app.data.purchase.model.PremiumFeature
import com.aptv.app.data.purchase.model.PurchaseState
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    activity: Activity,
    purchaseManager: PurchaseManager,
    purchaseStateFlow: StateFlow<PurchaseState>,
    onClose: () -> Unit
) {
    val purchaseState by purchaseStateFlow.collectAsState()
    val lifetimeProduct by purchaseManager.lifetimeProductDetails.collectAsState()
    val adFreeProduct by purchaseManager.adFreeProductDetails.collectAsState()
    val isBillingReady by purchaseManager.isBillingReady.collectAsState()

    var isPurchasing by remember { mutableStateOf(false) }
    var purchaseMessage by remember { mutableStateOf<String?>(null) }

    BackHandler(onBack = onClose)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("高级版", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1a1a2e)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (purchaseState.isPremium) {
                PremiumSuccessCard(purchaseState)
            } else {
                // 去广告选项（15元）
                if (!purchaseState.isAdFree) {
                    AdFreePurchaseCard(
                        product = adFreeProduct,
                        isBillingReady = isBillingReady,
                        purchaseState = purchaseState,
                        onPurchase = {
                            isPurchasing = true
                            purchaseManager.launchPurchaseFlow(
                                activity,
                                PurchaseState.PRODUCT_AD_FREE
                            ) { success, msg ->
                                isPurchasing = false
                                purchaseMessage = if (success) "购买成功！" else msg
                            }
                        },
                        isPurchasing = isPurchasing
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.layout.HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFCCCCCC)
                        )
                        Text(" 或 ", color = Color.Gray, fontSize = 14.sp)
                        androidx.compose.foundation.layout.HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFCCCCCC)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 终身高级版（45元）- 更明显的卡片
                LifetimePurchaseCard(
                    product = lifetimeProduct,
                    isBillingReady = isBillingReady,
                    onPurchase = {
                        isPurchasing = true
                        purchaseManager.launchPurchaseFlow(
                            activity,
                            PurchaseState.PRODUCT_LIFETIME
                        ) { success, msg ->
                            isPurchasing = false
                            purchaseMessage = if (success) "购买成功！" else msg
                        }
                    },
                    isPurchasing = isPurchasing
                )

                // 功能特性列表
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "高级版包含以下功能",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                FeatureItem(
                    title = "无限播放源",
                    description = "免费版仅允许添加 1 个播放源，高级版不限数量",
                    icon = Icons.Default.Star
                )

                FeatureItem(
                    title = "实时字幕识别",
                    description = "通过 AI 语音识别生成字幕，支持 Vosk / Whisper 多种引擎",
                    icon = Icons.Default.Star
                )

                FeatureItem(
                    title = "字幕翻译",
                    description = "支持 Google / 百度 / 腾讯 / OpenAI / DeepL 等多种翻译引擎",
                    icon = Icons.Default.Star
                )

                FeatureItem(
                    title = "多屏同播",
                    description = "支持最多 9 分屏同时播放多个频道",
                    icon = Icons.Default.Star
                )

                FeatureItem(
                    title = "云端同步",
                    description = "通过 Google Drive / Firebase 同步您的播放源与设置",
                    icon = Icons.Default.Star
                )

                FeatureItem(
                    title = "去除广告",
                    description = "纯净观影体验，不再显示任何广告",
                    icon = Icons.Default.Star
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 恢复购买
                OutlinedButton(
                    onClick = {
                        purchaseManager.restorePurchases { restored ->
                            purchaseMessage = if (restored) "已恢复购买！" else "未找到购买记录"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("恢复购买（重装设备后使用）")
                }

                purchaseMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        msg,
                        color = if (msg.startsWith("购买") || msg.startsWith("已")) Color(0xFF4CAF50) else Color.Red
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "支付将通过 Google Play 完成，订单与您的 Google 账户绑定。升级后终身可用。",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AdFreePurchaseCard(
    product: ProductDetails?,
    isBillingReady: Boolean,
    purchaseState: PurchaseState,
    onPurchase: () -> Unit,
    isPurchasing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "去广告版",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "永久去除 App 内所有广告",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                val priceText = product?.oneTimePurchaseOfferDetails?.formattedPrice ?: "¥15"
                Text(
                    priceText,
                    color = Color(0xFFe94560),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onPurchase,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isPurchasing && isBillingReady,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF888888)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isPurchasing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("购买去广告版")
            }
        }
    }
}

@Composable
private fun LifetimePurchaseCard(
    product: ProductDetails?,
    isBillingReady: Boolean,
    onPurchase: () -> Unit,
    isPurchasing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a1a2e)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF16213e),
                            Color(0xFF1a1a2e)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "APTV 终身高级版",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color(0xFFe94560),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "推荐",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "一次付费 · 终身享有所有功能",
                            fontSize = 13.sp,
                            color = Color(0xFFCCCCCC)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val priceText = product?.oneTimePurchaseOfferDetails?.formattedPrice ?: "¥45"
                        Text(
                            priceText,
                            color = Color(0xFFe94560),
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onPurchase,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isPurchasing && isBillingReady,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFe94560)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isPurchasing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        "立即升级高级版",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(description, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun PremiumSuccessCard(state: PurchaseState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "感谢购买 APTV 终身高级版！",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "您现在可以使用全部高级功能，包括实时字幕识别和多语言翻译。",
                fontSize = 13.sp,
                color = Color.Gray
            )
            if (state.purchaseTimeMs > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                val dateText = java.text.SimpleDateFormat("yyyy-MM-dd")
                    .format(java.util.Date(state.purchaseTimeMs))
                Text("购买时间：$dateText", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun PremiumGate(
    feature: PremiumFeature,
    purchaseState: PurchaseState,
    onGoPremium: () -> Unit,
    content: @Composable () -> Unit
) {
    val hasAccess = when (feature) {
        PremiumFeature.AD_FREE -> purchaseState.isAdFree || purchaseState.isPremium
        PremiumFeature.REAL_TIME_SUBTITLE,
        PremiumFeature.SUBTITLE_TRANSLATION,
        PremiumFeature.MULTI_SCREEN,
        PremiumFeature.CLOUD_SYNC,
        PremiumFeature.UNLIMITED_PLAYLISTS -> purchaseState.isPremium
    }

    if (hasAccess) {
        content()
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable(onClick = onGoPremium),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0)
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    "【高级版功能】${feature.displayName}",
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(feature.description, fontSize = 13.sp, color = Color(0xFF6B6B6B))
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = onGoPremium) {
                    Text("升级高级版")
                }
            }
        }
    }
}

@Composable
fun PremiumBadge(modifier: Modifier = Modifier) {
    Text(
        text = "PRO",
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(Color(0xFFe94560))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

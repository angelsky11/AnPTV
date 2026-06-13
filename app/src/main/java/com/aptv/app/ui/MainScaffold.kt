package com.aptv.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aptv.app.ads.AdmobManager
import com.aptv.app.data.purchase.PurchaseManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    purchaseManager: PurchaseManager,
    admobManager: AdmobManager,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val purchaseState by purchaseManager.purchaseState.collectAsState()
    val isAdFree = purchaseState.isAdFree

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!isAdFree) {
                    AdBannerContainer(
                        admobManager = admobManager
                    )
                }

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationItem.values().forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = { onNavigate(item.route) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            content()
        }
    }
}

@Composable
private fun AdBannerContainer(
    admobManager: AdmobManager
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color(0xFFf0f0f0)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                admobManager.createBannerAd()
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private enum class NavigationItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    HOME("home", "首页", Icons.Default.Home, Icons.Filled.Home),
    CHANNELS("channels", "频道", Icons.Default.Tv, Icons.Filled.Tv),
    FAVORITES("favorites", "收藏", Icons.Default.Star, Icons.Filled.Star),
    SETTINGS("settings", "设置", Icons.Default.Settings, Icons.Filled.Settings)
}

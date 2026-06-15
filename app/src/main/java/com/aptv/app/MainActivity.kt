package com.aptv.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aptv.app.data.iptv.IptvRepository
import com.aptv.app.data.iptv.model.Channel
import com.aptv.app.data.purchase.PurchaseManager
import com.aptv.app.ui.MainScaffold
import com.aptv.app.ui.channels.ChannelsScreen
import com.aptv.app.ui.favorites.FavoritesScreen
import com.aptv.app.ui.home.HomeScreen
import com.aptv.app.ui.player.PlayerScreen
import com.aptv.app.ui.premium.PremiumScreen
import com.aptv.app.ui.settings.SettingsScreen
import com.aptv.app.ui.theme.AptvTheme

class MainActivity : ComponentActivity() {

    private var currentChannel: Channel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AptvTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val app = (application as AptvApplication)
                    val purchaseManager = app.purchaseManager
                    val iptvRepository = app.iptvRepository
                    val navController = rememberNavController()

                    NavContent(
                        purchaseManager = purchaseManager,
                        iptvRepository = iptvRepository,
                        navController = navController,
                        activity = this,
                        onPlayChannel = { channel ->
                            currentChannel = channel
                            navController.navigate("player")
                        }
                    )
                }
            }
        }
    }

    fun getCurrentChannel(): Channel? = currentChannel
}

@Composable
private fun NavContent(
    purchaseManager: PurchaseManager,
    iptvRepository: IptvRepository,
    navController: NavHostController,
    activity: MainActivity,
    onPlayChannel: (Channel) -> Unit
) {
    val admobManager = (activity.application as AptvApplication).admobManager
    val currentRoute = navController.currentDestination?.route ?: "home"

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            MainScaffold(
                purchaseManager = purchaseManager,
                admobManager = admobManager,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            ) {
                HomeScreen(
                    iptvRepository = iptvRepository,
                    purchaseManager = purchaseManager,
                    onAddSource = { navController.navigate("settings") },
                    onGoPremium = { navController.navigate("premium") },
                    onPlayChannel = { channel -> onPlayChannel(channel) }
                )
            }
        }

        composable("channels") {
            MainScaffold(
                purchaseManager = purchaseManager,
                admobManager = admobManager,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            ) {
                ChannelsScreen(
                    iptvRepository = iptvRepository,
                    purchaseManager = purchaseManager,
                    onAddSource = { navController.navigate("settings") },
                    onGoPremium = { navController.navigate("premium") },
                    onPlayChannel = { channel -> onPlayChannel(channel) }
                )
            }
        }

        composable("favorites") {
            MainScaffold(
                purchaseManager = purchaseManager,
                admobManager = admobManager,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            ) {
                FavoritesScreen(
                    iptvRepository = iptvRepository,
                    onPlayChannel = { channel -> onPlayChannel(channel) }
                )
            }
        }

        composable("settings") {
            MainScaffold(
                purchaseManager = purchaseManager,
                admobManager = admobManager,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            ) {
                SettingsScreen(
                    iptvRepository = iptvRepository,
                    purchaseManager = purchaseManager,
                    onGoPremium = { navController.navigate("premium") }
                )
            }
        }

        composable("premium") {
            PremiumScreen(
                activity = activity,
                purchaseManager = purchaseManager,
                purchaseStateFlow = purchaseManager.purchaseState,
                onClose = { navController.popBackStack() }
            )
        }

        composable("player") {
            val channel = activity.getCurrentChannel()
            if (channel != null) {
                PlayerScreen(
                    channel = channel,
                    iptvRepository = iptvRepository,
                    purchaseManager = purchaseManager,
                    onBack = { navController.popBackStack() },
                    onGoPremium = { navController.navigate("premium") }
                )
            }
        }
    }
}

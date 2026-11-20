package com.ktp.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ktp.iptv.data.Channel
import com.ktp.iptv.ui.HomeScreen
import com.ktp.iptv.ui.MainViewModel
import com.ktp.iptv.ui.PlayerScreen
import com.ktp.iptv.ui.SettingsScreen
import com.ktp.iptv.ui.theme.KtpIptvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as KtpIptvApp
        setContent {
            KtpIptvTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    KtpIptvAppRoot(app)
                }
            }
        }
    }
}

private sealed class Screen {
    data object Home : Screen()
    data object Settings : Screen()
    data class Player(val channel: Channel) : Screen()
}

@Composable
fun KtpIptvAppRoot(app: KtpIptvApp) {
    val vm: MainViewModel = viewModel(
        factory = MainViewModel.factory(app.container.playlistRepository, app)
    )
    val state by vm.uiState.collectAsState()
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    // Chargement initial de la playlist publique
    LaunchedEffect(Unit) {
        if (state.channels.isEmpty() && !state.isLoading) {
            vm.loadDefault()
        }
        vm.checkUpdate()
    }

    when (val s = screen) {
        is Screen.Home -> HomeScreen(
            state = state,
            onChannelClick = { screen = Screen.Player(it) },
            onToggleFavorite = vm::toggleFavorite,
            onQueryChange = vm::setQuery,
            onCountrySelect = vm::setCountry,
            onCategorySelect = vm::setCategory,
            onLanguageSelect = vm::setLanguage,
            onRefresh = vm::loadDefault,
            onOpenSettings = { screen = Screen.Settings }
        )
        is Screen.Settings -> SettingsScreen(
            state = state,
            onBack = { screen = Screen.Home },
            onLoadDefault = {
                vm.loadDefault()
                screen = Screen.Home
            },
            onLoadM3u = {
                vm.loadM3u(it)
                screen = Screen.Home
            },
            onLoadXtream = { server, user, pass ->
                vm.loadXtream(server, user, pass)
                screen = Screen.Home
            },
            onProxyChange = vm::updateProxy,
            onCheckUpdate = vm::checkUpdate,
            onDownloadUpdate = vm::downloadAndInstallUpdate
        )
        is Screen.Player -> PlayerScreen(
            channel = s.channel,
            onBack = { screen = Screen.Home }
        )
    }
}

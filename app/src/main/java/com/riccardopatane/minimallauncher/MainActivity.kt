package com.riccardopatane.minimallauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riccardopatane.minimallauncher.ui.AppListScreen
import com.riccardopatane.minimallauncher.ui.FavoritesScreen
import com.riccardopatane.minimallauncher.ui.HomeScreen
import com.riccardopatane.minimallauncher.ui.LauncherViewModel
import com.riccardopatane.minimallauncher.ui.Screen
import com.riccardopatane.minimallauncher.ui.SettingsScreen
import com.riccardopatane.minimallauncher.ui.crt.CrtOverlay
import com.riccardopatane.minimallauncher.ui.crt.LocalCrtGlow
import com.riccardopatane.minimallauncher.ui.theme.LocalLauncherColors
import com.riccardopatane.minimallauncher.ui.theme.paletteFor

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    /** ACTION_USER_PRESENT fires on every unlock (not on screen-on). */
    private val userPresentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_PRESENT) {
                viewModel.onUserPresent()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(userPresentReceiver)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.onHomeIntent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        // system status bar hidden: no notification icons/system clock at the
        // top; a swipe from the top edge reveals it temporarily
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.statusBars())
        }
        registerReceiver(userPresentReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            var screen by rememberSaveable { mutableStateOf(Screen.Home) }
            BackHandler(enabled = screen != Screen.Home) {
                when (screen) {
                    Screen.FavoritesPicker -> screen = Screen.Settings
                    Screen.AppList -> {
                        viewModel.clearQuery()
                        screen = Screen.Home
                    }
                    Screen.Settings -> screen = Screen.Home
                    Screen.Home -> Unit
                }
            }
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
                // Home = home: on return (Home button, closed app) always
                // start back on the main screen, never on settings
                screen = Screen.Home
            }
            // HOME intent while the launcher is already on top: no resume, reset here
            val homeReset by viewModel.homeResetFlow.collectAsStateWithLifecycle()
            LaunchedEffect(homeReset) {
                if (homeReset > 0) {
                    viewModel.clearQuery()
                    screen = Screen.Home
                }
            }
            CompositionLocalProvider(
                LocalLauncherColors provides paletteFor(state.themeLevel),
                LocalCrtGlow provides state.crtGlow,
            ) {
                Box(Modifier.fillMaxSize()) {
                    when (screen) {
                        Screen.Home -> HomeScreen(
                            viewModel,
                            onOpenSettings = { screen = Screen.Settings },
                            onOpenAppList = { screen = Screen.AppList },
                        )
                        Screen.AppList -> AppListScreen(
                            viewModel,
                            onBack = {
                                viewModel.clearQuery()
                                screen = Screen.Home
                            },
                        )
                        Screen.Settings -> SettingsScreen(
                            viewModel,
                            onBack = { screen = Screen.Home },
                            onOpenFavorites = { screen = Screen.FavoritesPicker },
                        )
                        Screen.FavoritesPicker -> FavoritesScreen(
                            viewModel,
                            onBack = { screen = Screen.Settings },
                        )
                    }
                    // sopra TUTTE le schermate: griglia CRT (non intercetta tocchi)
                    CrtOverlay(state.crtGrid, Modifier.matchParentSize())
                }
            }
        }
    }
}

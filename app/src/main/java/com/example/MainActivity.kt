package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.audio.GlobalEqualizerService
import com.example.ui.EqualizerViewModel
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.PresetsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.SonicEqTheme
import com.example.ui.theme.TextSecondary
import kotlinx.serialization.Serializable

@Serializable object EqualizerRoute
@Serializable object PresetsRoute
@Serializable object PlayerRoute
@Serializable object SettingsRoute

private data class NavDestinationItem(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAudioControlPanelIntent(intent)

        setContent {
            SonicEqTheme(darkTheme = true) {
                SonicEqRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAudioControlPanelIntent(intent)
    }

    private fun handleAudioControlPanelIntent(intent: Intent?) {
        if (intent?.action == AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL) {
            val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
            val pkg = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME) ?: "External Audio App"
            if (sessionId > 0) {
                val app = applicationContext as? SonicEqApplication
                app?.audioEngine?.onExternalSessionOpened(sessionId, pkg)
                GlobalEqualizerService.ensureStarted(this)
            }
        }
    }
}

@Composable
private fun SonicEqRoot(
    viewModel: EqualizerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Gracefully request POST_NOTIFICATIONS on Android 13+ so foreground service notification displays
    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(uiState.feedbackMessage) {
        val msg = uiState.feedbackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearFeedback()
    }

    val destinations = remember {
        listOf(
            NavDestinationItem(
                route = EqualizerRoute,
                label = "Studio",
                selectedIcon = Icons.Rounded.Equalizer,
                unselectedIcon = Icons.Outlined.Equalizer,
                testTag = "nav_studio"
            ),
            NavDestinationItem(
                route = PresetsRoute,
                label = "Presets",
                selectedIcon = Icons.Rounded.LibraryMusic,
                unselectedIcon = Icons.Outlined.LibraryMusic,
                testTag = "nav_presets"
            ),
            NavDestinationItem(
                route = PlayerRoute,
                label = "Player",
                selectedIcon = Icons.Rounded.PlayCircle,
                unselectedIcon = Icons.Outlined.PlayCircle,
                testTag = "nav_player"
            ),
            NavDestinationItem(
                route = SettingsRoute,
                label = "Settings",
                selectedIcon = Icons.Rounded.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                testTag = "nav_settings"
            )
        )
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    fun navigateTo(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = StudioObsidian,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = StudioSurface
                    ) {
                        destinations.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyanContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = StudioSurface
                    ) {
                        destinations.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyanContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = EqualizerRoute
                    ) {
                        composable<EqualizerRoute> {
                            EqualizerScreen(
                                uiState = uiState,
                                onToggleEq = viewModel::toggleEqEnabled,
                                onToggleTenBand = viewModel::setUseTenBandEngine,
                                onTenBandChange = viewModel::updateTenBandGain,
                                onFiveBandChange = viewModel::updateFiveBandPair,
                                onToggleBassBoost = viewModel::toggleBassBoost,
                                onBassStrengthChange = viewModel::updateBassStrength,
                                onToggleVirtualizer = viewModel::toggleVirtualizer,
                                onVirtualizerStrengthChange = viewModel::updateVirtualizerStrength,
                                onToggleLoudness = viewModel::toggleLoudness,
                                onLoudnessGainChange = viewModel::updateLoudnessGain,
                                onPreampChange = viewModel::updatePreampDb,
                                onApplyPreset = viewModel::applyPreset,
                                onResetFlat = viewModel::resetToFlat,
                                onSavePreset = viewModel::saveCurrentCurveAsPreset,
                                onEnableVisualizer = viewModel::enableVisualizer
                            )
                        }
                        composable<PresetsRoute> {
                            PresetsScreen(
                                uiState = uiState,
                                onApplyPreset = { preset ->
                                    viewModel.applyPreset(preset)
                                    navigateTo(EqualizerRoute)
                                },
                                onDeletePreset = viewModel::deletePreset
                            )
                        }
                        composable<PlayerRoute> {
                            PlayerScreen(
                                uiState = uiState,
                                onAddAudioFiles = viewModel::addAudioFiles,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onSkipPrevious = viewModel::skipPrevious,
                                onSkipNext = viewModel::skipNext,
                                onSeekTo = viewModel::seekTo,
                                onPlayTrackAt = viewModel::playTrackAt,
                                onToggleReferenceTone = viewModel::toggleReferenceTone
                            )
                        }
                        composable<SettingsRoute> {
                            SettingsScreen(
                                uiState = uiState,
                                onToggleGlobalMix = viewModel::setGlobalOutputMixEnabled,
                                onToggleAutoAttach = viewModel::setAutoAttachExternalSessions,
                                onToggleTenBand = viewModel::setUseTenBandEngine,
                                onToggleLimiter = viewModel::setLimiterEnabled,
                                onTogglePersistentNotification = viewModel::setPersistentNotificationEnabled,
                                onAddCategory = viewModel::addCategory,
                                onDeleteCategory = viewModel::deleteCategory,
                                onAddPresetItem = viewModel::addPresetItem,
                                onUpdateItemNameAndImage = viewModel::updatePresetNameAndImage
                            )
                        }
                    }
                }
            }
        }
    }
}

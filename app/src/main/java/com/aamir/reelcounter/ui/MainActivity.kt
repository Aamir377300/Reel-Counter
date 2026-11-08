package com.aamir.reelcounter.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aamir.reelcounter.ReelCounterApp
import com.aamir.reelcounter.service.ReelTrackerService
import com.aamir.reelcounter.ui.screens.HistoryScreen
import com.aamir.reelcounter.ui.screens.HomeScreen
import com.aamir.reelcounter.ui.screens.SettingsScreen
import com.aamir.reelcounter.ui.screens.SetupScreen
import com.aamir.reelcounter.ui.theme.ReelCounterTheme
import com.aamir.reelcounter.util.PermissionHelpers

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReelCounterTheme {
                val settings by ReelCounterApp.get().preferences.settings
                    .collectAsStateWithLifecycle(initialValue = null)

                if (settings == null) return@ReelCounterTheme

                var showSetup by remember(settings!!.setupCompleted) {
                    mutableStateOf(
                        !settings!!.setupCompleted ||
                            !PermissionHelpers.isAccessibilityEnabled(this)
                    )
                }

                // If the service was disabled after setup, force the setup screen again.
                if (!ReelTrackerService.isRunning &&
                    !PermissionHelpers.isAccessibilityEnabled(this) &&
                    settings!!.setupCompleted
                ) {
                    showSetup = true
                }

                if (showSetup) {
                    SetupScreen(onFinished = { showSetup = false })
                } else {
                    MainScaffold()
                }
            }
        }
    }
}

private sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Dest("home", "Home", Icons.Default.Home)
    data object History : Dest("history", "History", Icons.Default.BarChart)
    data object Settings : Dest("settings", "Settings", Icons.Default.Settings)
}

@Composable
private fun MainScaffold() {
    val nav = rememberNavController()
    val items = listOf(Dest.Home, Dest.History, Dest.Settings)
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { dest ->
                    NavigationBarItem(
                        selected = current == dest.route,
                        onClick = {
                            nav.navigate(dest.route) {
                                popUpTo(Dest.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Dest.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Dest.Home.route) { HomeScreen() }
            composable(Dest.History.route) { HistoryScreen() }
            composable(Dest.Settings.route) { SettingsScreen() }
        }
    }
}
